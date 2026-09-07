package io.todorok.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    @Bean
    TraceIdCallableInterceptor traceIdCallableInterceptor() {
        return new TraceIdCallableInterceptor();
    }

    @Bean
    WebMvcConfigurer traceIdAsyncConfigurer(TraceIdCallableInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(
                    org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer configurer) {
                configurer.registerCallableInterceptors(interceptor);
            }
        };
    }
}
