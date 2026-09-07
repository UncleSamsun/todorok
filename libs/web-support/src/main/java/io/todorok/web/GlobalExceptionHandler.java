package io.todorok.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponseFactory problems;

    public GlobalExceptionHandler(ProblemResponseFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformedJson() {
        return response(400, "MALFORMED_JSON", "Malformed JSON", "The request body is not valid JSON.", false,
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException failure) {
        var fields = failure.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        return response(400, "VALIDATION_FAILED", "Validation failed", "One or more fields are invalid.", false,
                fields);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ProblemDetail> notFound() {
        return response(404, "NOT_FOUND", "Not found", "The requested resource was not found.", false, List.of());
    }

    @ExceptionHandler({ServletRequestBindingException.class, TypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalidRequest() {
        return response(400, "INVALID_REQUEST", "Invalid request",
                "A request parameter or path value is missing or invalid.", false, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> methodNotAllowed() {
        return response(405, "METHOD_NOT_ALLOWED", "Method not allowed",
                "The HTTP method is not supported for this resource.", false, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedMediaType() {
        return response(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type",
                "The request content type is not supported.", false, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> optimisticConflict() {
        return response(409, "OPTIMISTIC_LOCK_CONFLICT", "Conflict",
                "The resource was modified by another request.", false, List.of());
    }

    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<ProblemDetail> apiFailure(ApiFailure failure) {
        return response(failure.status(), failure.code(), failure.title(), failure.detail(), failure.retryable(),
                failure.fieldErrors());
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class})
    ResponseEntity<ProblemDetail> infrastructureUnavailable() {
        return response(503, "SERVICE_UNAVAILABLE", "Service unavailable",
                "The service is temporarily unavailable.", true, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception failure, HttpServletRequest request) {
        log.error("Unhandled request failure: {} {}", request.getMethod(), request.getRequestURI(), failure);
        return response(500, "INTERNAL_SERVER_ERROR", "Internal server error", "An unexpected error occurred.",
                false, List.of());
    }

    private ApiFailure.FieldError fieldError(FieldError field) {
        String code = field.getCode() == null ? "INVALID" : camelToUpperSnake(field.getCode());
        return new ApiFailure.FieldError(field.getField(), code, field.getDefaultMessage() == null
                ? "The field is invalid." : field.getDefaultMessage());
    }

    private String camelToUpperSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    private ResponseEntity<ProblemDetail> response(int status, String code, String title, String detail,
            boolean retryable, List<ApiFailure.FieldError> fieldErrors) {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        var body = problems.create(status, code, title, detail, retryable, fieldErrors,
                traceId == null ? "unavailable" : traceId);
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(body);
    }
}
