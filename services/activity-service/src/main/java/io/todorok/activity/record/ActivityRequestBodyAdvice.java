package io.todorok.activity.record;

import io.todorok.web.ApiFailure;
import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ControllerAdvice(assignableTypes = ActivityController.class)
final class ActivityRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private final JsonMapper strict = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final Map<String,String> MEMBERS = Map.of("NUMBER", "numberValue", "TIME", "timeSeconds",
        "SHORT_TEXT", "textValue", "CHECK", "checked", "MEMO", "memoValue");

    @Override public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) { return true; }

    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type type,
        Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        String encoding = input.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (encoding != null && !encoding.isBlank() && !encoding.trim().equalsIgnoreCase("identity"))
            throw new ApiFailure(415, "UNSUPPORTED_CONTENT_ENCODING", "Unsupported encoding", "Use identity encoding.", false);
        byte[] body = input.getBody().readNBytes(1_048_577);
        if (body.length > 1_048_576) throw new ApiFailure(413, "PAYLOAD_TOO_LARGE", "Payload too large", "The request body may be at most 1 MiB.", false);
        JsonNode tree;
        try { tree = strict.readTree(body); }
        catch (RuntimeException malformed) { throw ActivityTemplateRecords.invalid("MALFORMED_JSON", "Invalid JSON or duplicate key."); }
        var fields = tree.path("detail").path("study").get("fields");
        if (fields != null) {
            if (!fields.isArray()) throw ActivityTemplateRecords.invalid("FIELD_VALUE_INVALID", "fields must be an array.");
            for (var field : fields) {
                String member = MEMBERS.get(field.path("type").asText());
                if (!field.isObject() || member == null || field.size() != 3 || !field.path("fieldId").isString())
                    throw ActivityTemplateRecords.invalid("FIELD_VALUE_INVALID", "Supply fieldId, type and exactly one matching value member.");
                var value = field.get(member);
                if (value == null || switch (member) {
                    case "numberValue" -> !value.isNumber();
                    case "timeSeconds" -> !value.isIntegralNumber();
                    case "checked" -> !value.isBoolean();
                    default -> !value.isString();
                }) throw ActivityTemplateRecords.invalid("FIELD_VALUE_INVALID", "The JSON value must match the field type without coercion.");
            }
        }
        return new HttpInputMessage() {
            public InputStream getBody() { return new ByteArrayInputStream(body); }
            public HttpHeaders getHeaders() { return input.getHeaders(); }
        };
    }
}
