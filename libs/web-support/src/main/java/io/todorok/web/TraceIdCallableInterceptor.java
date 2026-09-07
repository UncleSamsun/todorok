package io.todorok.web;

import java.util.concurrent.Callable;
import org.slf4j.MDC;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;

final class TraceIdCallableInterceptor implements CallableProcessingInterceptor {
    private final ThreadLocal<String> previousTraceId = new ThreadLocal<>();

    @Override
    public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
        previousTraceId.set(MDC.get(TraceIdFilter.MDC_KEY));
        Object traceId = request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (traceId instanceof String value) {
            MDC.put(TraceIdFilter.MDC_KEY, value);
        } else {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }

    @Override
    public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
        String previous = previousTraceId.get();
        previousTraceId.remove();
        if (previous == null) {
            MDC.remove(TraceIdFilter.MDC_KEY);
        } else {
            MDC.put(TraceIdFilter.MDC_KEY, previous);
        }
    }
}
