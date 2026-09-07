package io.todorok.planner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=true", "spring.flyway.create-schemas=true",
    "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class AuthenticationIntegrationTest {
    static final java.security.KeyPair KEYS = keys();
    static java.security.KeyPair keys() {
        try { var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:17.11-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("todorok.auth.public-key", () -> java.util.Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
        r.add("todorok.auth.private-key", () -> java.util.Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded()));
        r.add("todorok.auth.allowed-origins", () -> "https://todorok.test");
    }
    @LocalServerPort int port;

    @Test void failedLoginUsesUnauthorizedProblemResponse() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/planner/v1/auth/login"))
            .header("Content-Type", "application/json").header("Origin", "https://todorok.test")
            .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"nobody@example.com\",\"password\":\"incorrect\"}"))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("UNAUTHORIZED", "traceId");
    }
}
