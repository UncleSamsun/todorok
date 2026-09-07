package io.todorok.notification.security;

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
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = NotificationSecurityIntegrationTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationSecurityIntegrationTest {
    static final KeyPair KEYS = keys();
    static final String USER = UUID.randomUUID().toString();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("todorok.auth.public-key", () -> Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
    }
    @LocalServerPort int port;
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;

    @Test void validatesPlannerStyleJwtWithOnlyPublicKeyAndSuppliesUuidPrincipal() throws Exception {
        assertThat(environment.getProperty("todorok.auth.private-key")).isNull();
        var response = request(token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", 300));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(USER);
    }

    @Test void rejectsMissingBearerWrongSignatureIssuerAudienceExpiryAndAlgorithmWithSharedProblems() throws Exception {
        assertUnauthorized(request(null));
        for (String token : List.of(
                token(keys(), JWSAlgorithm.RS256, "todorok", "todorok-api", 300),
                token(KEYS, JWSAlgorithm.RS256, "wrong", "todorok-api", 300),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "wrong", 300),
                token(KEYS, JWSAlgorithm.RS256, "todorok", "todorok-api", -1),
                token(KEYS, JWSAlgorithm.RS512, "todorok", "todorok-api", 300))) {
            assertUnauthorized(request(token));
        }
    }

    private void assertUnauthorized(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        var body = mapper.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("traceId").asText()).isEqualTo(response.headers().firstValue("X-Trace-Id").orElseThrow());
    }

    private HttpResponse<String> request(String jwt) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + environment.getProperty("server.servlet.context-path", "") + "/test/principal"));
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String token(KeyPair pair, JWSAlgorithm algorithm, String issuer, String audience, long seconds) throws Exception {
        var jwt = new SignedJWT(new JWSHeader(algorithm), new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject(USER).issueTime(new Date()).expirationTime(Date.from(java.time.Instant.now().plusSeconds(seconds))).build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) pair.getPrivate()));
        return jwt.serialize();
    }
    private static KeyPair keys() {
        try { var g = java.security.KeyPairGenerator.getInstance("RSA"); g.initialize(2048); return g.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({SecurityConfiguration.class, Probe.class})
    static class Application {}
    @RestController static class Probe {
        @GetMapping("/test/principal") String principal(@AuthenticationPrincipal UUID user) { return user.toString(); }
    }
}
