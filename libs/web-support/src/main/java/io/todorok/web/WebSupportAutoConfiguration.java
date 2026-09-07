package io.todorok.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class WebSupportAutoConfiguration {
    @Bean
    TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    ProblemResponseFactory problemResponseFactory() {
        return new ProblemResponseFactory();
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler(ProblemResponseFactory factory) {
        return new GlobalExceptionHandler(factory);
    }
}
