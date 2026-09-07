package io.todorok.activity.template;

import io.todorok.web.ApiFailure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

@ControllerAdvice(assignableTypes = TemplateController.class)
final class TemplateRequestBodyAdvice extends RequestBodyAdviceAdapter {
    static final int MAX_BODY_BYTES = 1_048_576;

    private final JsonMapper strictJson = JsonMapper.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .build();

    @Override
    public boolean supports(
        MethodParameter methodParameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return TemplateController.class.isAssignableFrom(methodParameter.getContainingClass());
    }

    @Override
    public HttpInputMessage beforeBodyRead(
        HttpInputMessage inputMessage,
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        String encoding = inputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (encoding != null && !encoding.isBlank() && !"identity".equalsIgnoreCase(encoding.trim())) {
            throw new ApiFailure(
                415,
                "UNSUPPORTED_CONTENT_ENCODING",
                "Unsupported content encoding",
                "Template management requests only support identity encoding.",
                false
            );
        }
        byte[] body = inputMessage.getBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new ApiFailure(
                413,
                "PAYLOAD_TOO_LARGE",
                "Payload too large",
                "Template management request bodies may be at most 1 MiB.",
                false
            );
        }
        try {
            strictJson.readTree(body);
        } catch (RuntimeException malformed) {
            throw new ApiFailure(
                400,
                "MALFORMED_JSON",
                "Malformed JSON",
                "The template management request body is not valid JSON or contains a duplicate key.",
                false
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(inputMessage.getHeaders());
        headers.setContentLength(body.length);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
