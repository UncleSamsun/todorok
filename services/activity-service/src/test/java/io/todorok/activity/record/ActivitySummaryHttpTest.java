package io.todorok.activity.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.todorok.activity.security.SecurityConfiguration;
import io.todorok.messaging.OutboxEventWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = ActivitySummaryHttpTest.Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivitySummaryHttpTest {
    static final KeyPair KEYS = keys();
    static final UUID USER = UUID.randomUUID();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("todorok.auth.public-key", () -> Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
    }
    @LocalServerPort int port;

    @Test void returnsProblemsForMalformedOutOfRangeTypeAndFutureQueries() throws Exception {
        for (String query : java.util.List.of(
            "month=not-a-month&activityType=STUDY",
            "month=2026-13&activityType=STUDY",
            "month=2026-09&activityType=RUNNING",
            "month=2026-10&activityType=STUDY")) {
            var response = request(query);
            assertThat(response.statusCode()).as(query).isEqualTo(400);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).as(query)
                .startsWith("application/problem+json");
        }
        assertThat(request("month=2026-09&activityType=STUDY").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> request(String query) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
            URI.create("http://localhost:" + port + "/api/activity/v1/activities/summary?" + query))
            .header("Authorization", "Bearer " + token()).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String token() throws Exception {
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
            .issuer("todorok").audience("todorok-api").subject(USER.toString()).issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) KEYS.getPrivate()));
        return jwt.serialize();
    }
    private static KeyPair keys() {
        try { var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({SecurityConfiguration.class, ActivityController.class})
    static class Application {
        @Bean ActivityService activities() {
            var jdbc = mock(JdbcTemplate.class);
            when(jdbc.queryForMap(any(String.class), any(), any(), any(), any()))
                .thenReturn(Map.of("completed_count", 0, "duration_seconds", 0L));
            return new ActivityService(jdbc, mock(ActivityDetailStore.class), mock(OutboxEventWriter.class),
                mock(ObjectMapper.class), Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneOffset.UTC));
        }
        @Bean io.todorok.activity.program.ProgramCatalogStore catalogs() {
            return mock(io.todorok.activity.program.ProgramCatalogStore.class);
        }
        @Bean io.todorok.activity.program.ProgramEnrollmentService enrollments() {
            return mock(io.todorok.activity.program.ProgramEnrollmentService.class);
        }
    }
}
