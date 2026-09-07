package io.todorok.web.security;

import io.todorok.web.ProblemResponseFactory;
import io.todorok.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

public final class SecuritySupport {
    private SecuritySupport() {}

    public static JwtDecoder decoder(Environment environment) {
        var decoder = NimbusJwtDecoder.withPublicKey(RsaKeys.publicKey(environment))
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        String issuer = environment.getRequiredProperty("todorok.auth.issuer");
        String audience = environment.getRequiredProperty("todorok.auth.audience");
        if (issuer.isBlank() || audience.isBlank()) throw new IllegalStateException("JWT issuer and audience are required");
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(issuer), jwt -> {
                    try {
                        UUID id = UUID.fromString(jwt.getSubject());
                        if (!id.toString().equals(jwt.getSubject()) || jwt.getExpiresAt() == null
                                || jwt.getIssuedAt() == null || !jwt.getAudience().contains(audience)) {
                            return invalid();
                        }
                        return OAuth2TokenValidatorResult.success();
                    } catch (RuntimeException e) { return invalid(); }
                }));
        return decoder;
    }

    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }

    public static void base(HttpSecurity http, JwtDecoder decoder, ProblemResponseFactory factory,
            JsonMapper mapper) throws Exception {
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, failure) -> problem(response, factory, mapper, 401))
                .accessDeniedHandler((request, response, failure) -> problem(response, factory, mapper, 403)))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint((request, response, failure) -> problem(response, factory, mapper, 401))
                .accessDeniedHandler((request, response, failure) -> problem(response, factory, mapper, 403))
                .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(jwt ->
                    new UsernamePasswordAuthenticationToken(UUID.fromString(jwt.getSubject()), null, List.of()))));
    }

    public static SecurityFilterChain resourceChain(HttpSecurity http, JwtDecoder decoder,
            ProblemResponseFactory factory, JsonMapper mapper) throws Exception {
        base(http, decoder, factory, mapper);
        // Resource endpoints never authenticate cookies. Unsafe requests still require explicit bearer credentials.
        http.csrf(c -> c.ignoringRequestMatchers(SecuritySupport::hasBearer))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

    public static boolean hasBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    public static void problem(HttpServletResponse response, ProblemResponseFactory factory,
            JsonMapper mapper, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        String trace = MDC.get(TraceIdFilter.MDC_KEY);
        var problem = factory.create(status, status == 401 ? "UNAUTHORIZED" : "FORBIDDEN",
                status == 401 ? "Unauthorized" : "Forbidden", "The request could not be authorized.",
                false, List.of(), trace == null ? "unavailable" : trace);
        response.getWriter().write(mapper.writeValueAsString(problem));
    }
}
