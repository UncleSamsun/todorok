package io.todorok.web;

import java.util.List;

public final class ApiFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final String title;
    private final String detail;
    private final boolean retryable;
    private final List<FieldError> fieldErrors;

    public ApiFailure(int status, String code, String title, String detail, boolean retryable) {
        this(status, code, title, detail, retryable, List.of());
    }

    public ApiFailure(int status, String code, String title, String detail, boolean retryable,
            List<FieldError> fieldErrors) {
        super(code);
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be an HTTP error status");
        }
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.retryable = retryable;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public int status() { return status; }
    public String code() { return code; }
    public String title() { return title; }
    public String detail() { return detail; }
    public boolean retryable() { return retryable; }
    public List<FieldError> fieldErrors() { return fieldErrors; }

    public record FieldError(String field, String code, String message) {}
}
