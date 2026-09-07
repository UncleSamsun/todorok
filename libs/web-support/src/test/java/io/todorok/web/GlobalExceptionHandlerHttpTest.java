package io.todorok.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = GlobalExceptionHandlerHttpTest.TestApplication.class)
class GlobalExceptionHandlerHttpTest {
    private final MockMvc mvc;
    private final AsyncTaskExecutor asyncExecutor;

    @Autowired
    GlobalExceptionHandlerHttpTest(WebApplicationContext context) {
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(TraceIdFilter.class))
                .build();
        this.asyncExecutor = context.getBean("testMvcExecutor", AsyncTaskExecutor.class);
    }

    @Test
    void malformedJsonIsSafe400Problem() throws Exception {
        assertProblem(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{"),
                400, "MALFORMED_JSON", false);
    }

    @Test
    void validationFailureIncludesFieldErrors() throws Exception {
        mvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NOT_BLANK"));
    }

    @Test
    void missingRouteIs404Problem() throws Exception {
        assertProblem(get("/route-that-does-not-exist"), 404, "NOT_FOUND", false);
    }

    @Test
    void invalidOrMissingQueryValuesAre400Problems() throws Exception {
        assertProblem(get("/test/query"), 400, "INVALID_REQUEST", false);
        assertProblem(get("/test/query").queryParam("count", "not-a-number"), 400, "INVALID_REQUEST", false);
        assertProblem(get("/test/path/not-a-number"), 400, "INVALID_REQUEST", false);
    }

    @Test
    void unsupportedMethodAndMediaTypeRemain4xxProblems() throws Exception {
        assertProblem(put("/test/query"), 405, "METHOD_NOT_ALLOWED", false);
        assertProblem(post("/test/validate").contentType(MediaType.TEXT_PLAIN).content("name"),
                415, "UNSUPPORTED_MEDIA_TYPE", false);
    }

    @Test
    void optimisticConflictIs409Problem() throws Exception {
        assertProblem(get("/test/optimistic"), 409, "OPTIMISTIC_LOCK_CONFLICT", false);
    }

    @Test
    void businessFailureIs422Problem() throws Exception {
        assertProblem(get("/test/business"), 422, "RULE_REJECTED", false);
    }

    @Test
    void transientInfrastructureFailureIsRetryable503Problem() throws Exception {
        assertProblem(get("/test/infrastructure"), 503, "SERVICE_UNAVAILABLE", true);
    }

    @Test
    void unexpectedFailureIsSanitized500Problem() throws Exception {
        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("secret SQL", "IllegalStateException"));
    }

    @Test
    void generatedTraceIdOverridesInboundHeaderAndIsClearedAfterRequest() throws Exception {
        var result = mvc.perform(get("/test/business").header(TraceIdFilter.HEADER_NAME, "attacker-value"))
                .andExpect(header().exists(TraceIdFilter.HEADER_NAME))
                .andReturn();
        var traceId = result.getResponse().getHeader(TraceIdFilter.HEADER_NAME);
        assertThat(traceId).isNotEqualTo("attacker-value");
        assertThat(result.getResponse().getContentAsString()).contains("\"traceId\":\"" + traceId + "\"");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void asyncFailureUsesTheOriginalTraceIdInHeaderAndBody() throws Exception {
        var initial = mvc.perform(get("/test/async-failure"))
                .andExpect(request().asyncStarted())
                .andReturn();
        var traceId = initial.getResponse().getHeader(TraceIdFilter.HEADER_NAME);

        mvc.perform(asyncDispatch(initial))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(TraceIdFilter.HEADER_NAME, traceId))
                .andExpect(jsonPath("$.traceId").value(traceId));
        assertThat(traceId).isNotEqualTo("unavailable");
        assertThat(TestApplication.TestController.workerTrace.get()).isEqualTo(traceId);
        assertThat(asyncExecutor.submit(() -> MDC.get(TraceIdFilter.MDC_KEY)).get(5, TimeUnit.SECONDS)).isNull();
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void filterRestoresTheMdcValueThatWasPresentBeforeDispatch() throws Exception {
        MDC.put(TraceIdFilter.MDC_KEY, "outer-trace");
        try {
            var result = mvc.perform(get("/test/business")).andReturn();
            assertThat(result.getResponse().getHeader(TraceIdFilter.HEADER_NAME)).isNotEqualTo("outer-trace");
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo("outer-trace");
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }

    @Test
    void factorySerializesUnauthorizedAndForbiddenContracts() {
        var factory = new ProblemResponseFactory();
        assertThat(factory.create(401, "UNAUTHORIZED", "Unauthorized", "Authentication is required.", false, List.of(), "trace-1")
                .getProperties()).containsEntry("code", "UNAUTHORIZED").containsEntry("traceId", "trace-1");
        assertThat(factory.create(403, "FORBIDDEN", "Forbidden", "Access is denied.", false, List.of(), "trace-2")
                .getProperties()).containsEntry("code", "FORBIDDEN").containsEntry("traceId", "trace-2");
    }

    @Test
    void unauthorizedAndForbiddenUseTheProblemFactoryContract() throws Exception {
        assertProblem(get("/test/unauthorized"), 401, "UNAUTHORIZED", false);
        assertProblem(get("/test/forbidden"), 403, "FORBIDDEN", false);
    }

    private void assertProblem(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            int status, String code, boolean retryable) throws Exception {
        mvc.perform(request)
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(status))
                .andExpect(header().exists(TraceIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.retryable").value(retryable));
    }

    @SpringBootApplication
    static class TestApplication {
        @Bean("testMvcExecutor")
        ThreadPoolTaskExecutor testMvcExecutor() {
            var executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setThreadNamePrefix("mvc-test-");
            return executor;
        }

        @Bean
        WebMvcConfigurer testAsyncConfigurer(ThreadPoolTaskExecutor testMvcExecutor) {
            return new WebMvcConfigurer() {
                @Override
                public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                    configurer.setTaskExecutor(testMvcExecutor);
                }
            };
        }

        @RestController
        static class TestController {
            static final AtomicReference<String> workerTrace = new AtomicReference<>();
            @PostMapping("/test/validate")
            void validate(@Valid @RequestBody Input input) {}

            @GetMapping("/test/optimistic")
            void optimistic() { throw new OptimisticLockingFailureException("internal row version"); }

            @GetMapping("/test/query")
            int query(@RequestParam("count") int count) { return count; }

            @GetMapping("/test/path/{count}")
            int path(@PathVariable("count") int count) { return count; }

            @GetMapping("/test/business")
            void business() { throw new ApiFailure(422, "RULE_REJECTED", "Rule rejected", "The request violates a business rule.", false); }

            @GetMapping("/test/infrastructure")
            void infrastructure() { throw new TransientDataAccessResourceException("jdbc:postgresql://secret"); }

            @GetMapping("/test/unauthorized")
            void unauthorized() { throw new ApiFailure(401, "UNAUTHORIZED", "Unauthorized", "Authentication is required.", false); }

            @GetMapping("/test/forbidden")
            void forbidden() { throw new ApiFailure(403, "FORBIDDEN", "Forbidden", "Access is denied.", false); }

            @GetMapping("/test/unexpected")
            void unexpected() { throw new IllegalStateException("secret SQL token"); }

            @GetMapping("/test/async-failure")
            Callable<Void> asyncFailure() {
                return () -> {
                    workerTrace.set(MDC.get(TraceIdFilter.MDC_KEY));
                    throw new ApiFailure(422, "ASYNC_RULE_REJECTED", "Rule rejected",
                            "The asynchronous request violates a business rule.", false);
                };
            }
        }

        record Input(@NotBlank String name) {}
    }
}
