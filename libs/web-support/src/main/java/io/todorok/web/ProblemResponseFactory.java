package io.todorok.web;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemResponseFactory {
    public ProblemDetail create(int status, String code, String title, String detail, boolean retryable,
            List<ApiFailure.FieldError> fieldErrors, String traceId) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("traceId", traceId);
        problem.setProperty("retryable", retryable);
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return problem;
    }
}
