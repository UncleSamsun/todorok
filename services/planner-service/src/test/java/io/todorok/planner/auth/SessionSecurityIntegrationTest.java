package io.todorok.planner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(SessionSecurityIntegrationTest.ProbeConfiguration.class)
class SessionSecurityIntegrationTest extends AuthenticationIntegrationTest {
    static final String EMAIL = "owner@example.com";
    static final String PASSWORD = "valid-password-1234";
    static final String ORIGIN = "https://todorok.test";
    private final HttpClient client = HttpClient.newHttpClient();
    @Autowired UserAccount users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @BeforeEach void clearAccounts() {
        jdbc.execute("truncate planner.user_account, planner.refresh_family, planner.refresh_session, planner.login_attempt_bucket cascade");
    }

    @Test void bootstrapIsIdempotentAndKeepsTheOriginalStrongPasswordHash() {
        bootstrapCommand(EMAIL, PASSWORD);
        UUID first = users.find(EMAIL).orElseThrow().id();
        bootstrapCommand("another@example.com", "another-password-123");
        UUID second = users.find(EMAIL).orElseThrow().id();
        assertThat(second).isEqualTo(first);
        assertThat(jdbc.queryForObject("select count(*) from planner.user_account", Integer.class)).isOne();
        String hash = users.find(EMAIL).orElseThrow().passwordHash();
        assertThat(hash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        assertThat(passwords.matches("another-password-123", hash)).isFalse();
    }

    private void bootstrapCommand(String email, String password) {
        BootstrapCommand.main(new String[] {
                "--spring.datasource.url=" + DB.getJdbcUrl(), "--spring.datasource.username=" + DB.getUsername(),
                "--spring.datasource.password=" + DB.getPassword(), "--spring.flyway.enabled=false",
                "--TODOROK_BOOTSTRAP_EMAIL=" + email, "--TODOROK_BOOTSTRAP_PASSWORD=" + password
        });
    }

    @Test void loginIssuesRs256AccessAndOnlyHashedRefreshWithSecureCookie() throws Exception {
        UUID user = users.bootstrap(EMAIL, PASSWORD, passwords);
        var login = login();
        assertThat(login.statusCode()).isEqualTo(200);
        var body = body(login);
        var jwt = SignedJWT.parse(body.get("accessToken").asText());
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(user.toString());
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()
                .getEpochSecond() - jwt.getJWTClaimsSet().getIssueTime().toInstant().getEpochSecond()).isEqualTo(600);
        assertThat(login.headers().firstValue("cache-control")).contains("no-store");
        assertCookie(login, false);
        String hash = jdbc.queryForObject("select token_hash from planner.refresh_session", String.class);
        assertThat(hash).hasSize(64).doesNotContain(cookie(login).substring(cookie(login).indexOf('=') + 1));
        var me = send("GET", "/test/me", null, null, body.get("accessToken").asText(), null);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(body(me).get("userId").asText()).isEqualTo(user.toString());
    }

    @Test void rotationReplayRevokesFamilyEvenAfterFailureResponseButNotOtherFamilies() throws Exception {
        users.bootstrap(EMAIL, PASSWORD, passwords);
        var first = login();
        var independent = login();
        var rotated = refresh(cookie(first));
        assertThat(rotated.statusCode()).isEqualTo(200);
        assertCookie(rotated, false);
        assertThat(cookie(rotated)).isNotEqualTo(cookie(first));
        assertProblem(refresh(cookie(first)), 401, "UNAUTHORIZED");
        assertThat(jdbc.queryForObject("select count(*) from planner.refresh_family where revoked_at is not null", Integer.class)).isOne();
        assertProblem(refresh(cookie(rotated)), 401, "UNAUTHORIZED");
        assertThat(refresh(cookie(independent)).statusCode()).isEqualTo(200);
    }

    @Test void concurrentRefreshConsumesOnceAndCommitsReplayRevocation() throws Exception {
        users.bootstrap(EMAIL, PASSWORD, passwords);
        String original = cookie(login());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return refresh(original); });
            var b = executor.submit(() -> { start.await(); return refresh(original); });
            start.countDown();
            var left = a.get(20, java.util.concurrent.TimeUnit.SECONDS);
            var right = b.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(List.of(left.statusCode(), right.statusCode())).containsExactlyInAnyOrder(200, 401);
            assertThat(jdbc.queryForObject("select count(*) from planner.refresh_session", Integer.class)).isEqualTo(2);
            var winner = left.statusCode() == 200 ? left : right;
            assertProblem(refresh(cookie(winner)), 401, "UNAUTHORIZED");
        }
    }

    @Test void expiredAndMalformedRefreshCannotIssueSession() throws Exception {
        users.bootstrap(EMAIL, PASSWORD, passwords);
        var login = login();
        jdbc.update("update planner.refresh_session set expires_at = now() - interval '1 second'");
        assertProblem(refresh(cookie(login)), 401, "UNAUTHORIZED");
        assertProblem(refresh("todorok_refresh=invalid"), 401, "UNAUTHORIZED");
        assertProblem(refresh(null), 401, "UNAUTHORIZED");
        assertThat(jdbc.queryForObject("select count(*) from planner.refresh_session", Integer.class)).isOne();
    }

    @Test void logoutRequiresBearerDeletesMatchingCookieAndRevokesTheFamily() throws Exception {
        users.bootstrap(EMAIL, PASSWORD, passwords);
        var first = login();
        assertProblem(send("POST", "/auth/logout", null, cookie(first), null, ORIGIN), 401, "UNAUTHORIZED");
        var rotated = refresh(cookie(first));
        var logout = send("POST", "/auth/logout", null, cookie(first), access(rotated), ORIGIN);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertCookie(logout, true);
        var failure = refresh(cookie(rotated));
        assertProblem(failure, 401, "UNAUTHORIZED");
        assertCookie(failure, true);
    }

    @Test void wrongOwnerLogoutCannotRevokeAnotherUsersFamily() throws Exception {
        users.bootstrap(EMAIL, PASSWORD, passwords);
        var login = login();
        var otherUserToken = token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", UUID.randomUUID().toString(), Instant.now().plusSeconds(300));
        assertThat(send("POST", "/auth/logout", null, cookie(login), otherUserToken, ORIGIN).statusCode()).isEqualTo(204);
        assertThat(refresh(cookie(login)).statusCode()).isEqualTo(200);
    }

    @Test void originIsRequiredAndCannotBeReplacedByForgedHostHeaders() throws Exception {
        for (String origin : new String[] {null, "null", "https://evil.test", "https://todorok.test.evil.test"}) {
            assertProblem(send("POST", "/auth/login", credentials(), null, null, origin), 403, "FORBIDDEN");
            assertProblem(send("POST", "/auth/refresh", null, null, null, origin), 403, "FORBIDDEN");
        }
        var request = HttpRequest.newBuilder(url("/auth/refresh")).header("X-Forwarded-Host", "todorok.test")
                .header("X-Forwarded-Proto", "https").POST(HttpRequest.BodyPublishers.noBody()).build();
        assertProblem(client.send(request, HttpResponse.BodyHandlers.ofString()), 403, "FORBIDDEN");
    }

    @Test void bearerValidationRejectsWrongSignatureClaimsAlgorithmAndMissingExpiry() throws Exception {
        assertProblem(send("GET", "/test/me", null, null, null, null), 401, "UNAUTHORIZED");
        String user = UUID.randomUUID().toString();
        Instant future = Instant.now().plusSeconds(300);
        List<String> invalid = List.of(
                token(keys(), JWSAlgorithm.RS256, "todorok", "todorok-api", user, future),
                token(KEYS, JWSAlgorithm.RS256, "other", "todorok-api", user, future),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "other", user, future),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", user, Instant.now().minusSeconds(1)),
                token(KEYS, JWSAlgorithm.RS512, "todorok", "todorok-api", user, future),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", "not-a-uuid", future),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", user, null));
        for (String jwt : invalid) assertProblem(send("GET", "/test/me", null, null, jwt, null), 401, "UNAUTHORIZED");
    }

    @Test void repeatedLoginReturnsShared429WithRetryAfterAndWindowCanRecover() throws Exception {
        for (int i = 0; i < 5; i++) assertProblem(login(), 401, "UNAUTHORIZED");
        var blocked = login();
        assertProblem(blocked, 429, "RATE_LIMITED");
        assertThat(blocked.headers().firstValue("Retry-After")).contains("60");
        assertThat(body(blocked).get("retryable").asBoolean()).isTrue();
        jdbc.update("update planner.login_attempt_bucket set window_start = now() - interval '61 seconds'");
        assertProblem(login(), 401, "UNAUTHORIZED");
    }

    private String token(KeyPair key, JWSAlgorithm algorithm, String issuer, String audience, String user, Instant expiry) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(user).issueTime(new Date());
        if (expiry != null) claims.expirationTime(Date.from(expiry));
        var jwt = new SignedJWT(new JWSHeader(algorithm), claims.build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) key.getPrivate()));
        return jwt.serialize();
    }

    private HttpResponse<String> login() throws Exception { return send("POST", "/auth/login", credentials(), null, null, ORIGIN); }
    private String credentials() { return "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"; }
    private HttpResponse<String> refresh(String cookie) throws Exception { return send("POST", "/auth/refresh", null, cookie, null, ORIGIN); }
    private JsonNode body(HttpResponse<String> response) { return json.readTree(response.body()); }
    private String access(HttpResponse<String> response) { return body(response).get("accessToken").asText(); }
    private String cookie(HttpResponse<String> response) { return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0]; }
    private URI url(String path) { return URI.create("http://localhost:" + port + "/api/planner/v1" + path); }
    private HttpResponse<String> send(String method, String path, String content, String cookie, String bearer, String origin) throws Exception {
        var builder = HttpRequest.newBuilder(url(path)).timeout(java.time.Duration.ofSeconds(15));
        if (content != null) builder.header("Content-Type", "application/json");
        if (cookie != null) builder.header("Cookie", cookie);
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (origin != null) builder.header("Origin", origin);
        builder.method(method, content == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(content));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(body(response).get("code").asText()).isEqualTo(code);
        assertThat(body(response).get("traceId").asText()).isEqualTo(response.headers().firstValue("X-Trace-Id").orElseThrow());
    }
    private void assertCookie(HttpResponse<String> response, boolean deleted) {
        String header = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(header).contains("Path=/api/planner/v1/auth", "HttpOnly", "Secure", "SameSite=Lax",
                deleted ? "Max-Age=0" : "Max-Age=2592000");
    }

    @TestConfiguration(proxyBeanMethods = false) static class ProbeConfiguration {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }
    @RestController static class ProbeController {
        @GetMapping("/test/me") Map<String, UUID> me(@AuthenticationPrincipal UUID user) { return Map.of("userId", user); }
    }
}
