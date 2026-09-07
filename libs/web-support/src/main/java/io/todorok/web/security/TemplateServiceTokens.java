package io.todorok.web.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

/** A separate identity and key space from user access tokens. */
public final class TemplateServiceTokens {
    public static final String PATH = "/api/activity/v1/internal/template-selections";
    private TemplateServiceTokens() {}

    private static Environment keys(Environment env, String kind) {
        var keys = new StandardEnvironment();
        keys.getPropertySources().addFirst(new MapPropertySource("template-service", Map.of(
            "todorok.auth." + kind + "-key", env.getProperty("todorok.template-service." + kind + "-key", ""),
            "todorok.auth." + kind + "-key-location", env.getProperty("todorok.template-service." + kind + "-key-location", ""))));
        return keys;
    }

    public static JwtDecoder decoder(Environment env) {
        if (env.getProperty("todorok.template-service.public-key", "").isBlank()
            && env.getProperty("todorok.template-service.public-key-location", "").isBlank()) {
            return token -> { throw new BadJwtException("Service authentication is unavailable"); };
        }
        var key = RsaKeys.publicKey(keys(env, "public"));
        if (key.getModulus().equals(RsaKeys.publicKey(env).getModulus()))
            throw new IllegalStateException("Service and user keys must be separate");
        var decoder = NimbusJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO), jwt -> {
            boolean valid = "todorok-planner".equals(jwt.getClaimAsString("iss"))
                && "todorok-planner".equals(jwt.getSubject())
                && jwt.getAudience().equals(List.of("todorok-activity-internal"))
                && "template:select".equals(jwt.getClaimAsString("scope"))
                && "POST".equals(jwt.getClaimAsString("method")) && PATH.equals(jwt.getClaimAsString("path"))
                && jwt.getIssuedAt() != null && jwt.getExpiresAt() != null
                && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                && !jwt.getIssuedAt().isAfter(Instant.now())
                && Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds() <= 60;
            return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }));
        return decoder;
    }

    public static String sign(Environment env, String body, Map<String,String> claims) {
        try {
            var privateKey = RsaKeys.privateKey(keys(env, "private"));
            if (privateKey.getModulus().equals(RsaKeys.publicKey(env).getModulus()))
                throw new IllegalStateException("Service and user keys must be separate");
            Instant now = Instant.now();
            var builder = new JWTClaimsSet.Builder().issuer("todorok-planner").subject("todorok-planner")
                .audience("todorok-activity-internal").issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(60)))
                .claim("scope", "template:select").claim("method", "POST").claim("path", PATH)
                .claim("fingerprint", hash(body));
            claims.forEach(builder::claim);
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build());
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (Exception failure) { throw new IllegalStateException("Service authentication is unavailable"); }
    }

    public static String hash(String body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
}
