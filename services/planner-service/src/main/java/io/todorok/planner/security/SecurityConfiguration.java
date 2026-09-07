package io.todorok.planner.security;

import io.todorok.planner.auth.AuthService;
import io.todorok.planner.auth.RefreshSession;
import io.todorok.planner.auth.UserAccount;
import io.todorok.web.ProblemResponseFactory;
import io.todorok.web.security.RsaKeys;
import io.todorok.web.security.SecuritySupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean JwtDecoder jwtDecoder(Environment env) { return SecuritySupport.decoder(env); }
    @Bean AuthService authService(UserAccount users, RefreshSession sessions, PasswordEncoder encoder, Environment env) {
        var privateKey = RsaKeys.privateKey(env);
        if (!privateKey.getModulus().equals(RsaKeys.publicKey(env).getModulus())) {
            throw new IllegalStateException("JWT signing and verification keys do not match");
        }
        return new AuthService(users, sessions, encoder, privateKey, env);
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            ProblemResponseFactory factory, JsonMapper mapper, Environment env) throws Exception {
        Set<String> origins = Arrays.stream(env.getRequiredProperty("todorok.auth.allowed-origins").split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        if (origins.isEmpty()) throw new IllegalStateException("An explicit Origin allowlist is required");
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if ((!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getPath().isEmpty()) throw new IllegalStateException("Invalid allowed Origin");
        }
        SecuritySupport.base(http, decoder, factory, mapper);
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.copyOf(origins)); cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("X-Trace-Id", "Retry-After"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", cors);
        http.cors(c -> c.configurationSource(source));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String origin = request.getHeader("Origin");
                if ((cookieWrite(request) || origin != null)
                        && (origin == null || !origins.contains(origin)
                            || java.util.Collections.list(request.getHeaders("Origin")).size() != 1)) {
                    SecuritySupport.problem(response, factory, mapper, 403); return;
                }
                chain.doFilter(request, response);
            }
        }, CorsFilter.class);
        // For cookie writes, strict Origin validation is the CSRF defense. All other unsafe routes
        // retain Spring CSRF protection unless explicit bearer credentials are supplied.
        http.csrf(c -> c.ignoringRequestMatchers(request ->
                (cookieWrite(request) && origins.contains(request.getHeader("Origin")))
                        || (!cookieWrite(request) && SecuritySupport.hasBearer(request))))
            .authorizeHttpRequests(a -> a.requestMatchers("/auth/login", "/auth/refresh",
                    "/actuator/health", "/actuator/health/**").permitAll().anyRequest().authenticated());
        return http.build();
    }

    private static boolean cookieWrite(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equals(request.getMethod()) && Set.of("/auth/login", "/auth/refresh", "/auth/logout").contains(path);
    }
}
