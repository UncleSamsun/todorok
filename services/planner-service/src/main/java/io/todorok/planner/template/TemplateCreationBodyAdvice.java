package io.todorok.planner.template;

import io.todorok.planner.api.model.CreateSeriesRequest;
import io.todorok.planner.api.model.CreateTaskRequest;
import io.todorok.planner.series.SeriesController;
import io.todorok.planner.task.TaskController;
import io.todorok.web.ApiFailure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** Validate the selection's JSON number before the existing mapper can coerce a string or fraction into Long. */
@ControllerAdvice(assignableTypes = {TaskController.class, SeriesController.class})
public final class TemplateCreationBodyAdvice extends RequestBodyAdviceAdapter {
    private final JsonMapper mapper=JsonMapper.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    @Override public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converter) {
        return targetType==CreateTaskRequest.class || targetType==CreateSeriesRequest.class;
    }

    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        byte[] body=input.getBody().readAllBytes();
        try {
            var root=mapper.readTree(body);
            if (root!=null && root.hasNonNull("templateSelection")) {
                var version=root.path("templateSelection").path("expectedTemplateVersion");
                if (!version.isIntegralNumber() || !version.canConvertToLong()) throw new ApiFailure(400,"VALIDATION_FAILED",
                    "Invalid template version","expectedTemplateVersion must be an integer JSON number.",false,
                    List.of(new ApiFailure.FieldError("templateSelection.expectedTemplateVersion","VALIDATION_FAILED","정수 버전을 입력해 주세요.")));
            }
        } catch (JacksonException malformed) {
            throw new ApiFailure(400,"MALFORMED_JSON","Malformed JSON","The request body is not valid JSON.",false);
        }
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
            @Override public HttpHeaders getHeaders() { return input.getHeaders(); }
        };
    }
}
