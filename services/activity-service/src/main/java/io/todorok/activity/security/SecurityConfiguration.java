package io.todorok.activity.security;

import io.todorok.web.ProblemResponseFactory;
import io.todorok.web.security.SecuritySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class SecurityConfiguration {
    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain internalSecurityFilterChain(HttpSecurity http, Environment env,
            ProblemResponseFactory factory, JsonMapper mapper) throws Exception {
        http.securityMatcher("/internal/**")
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((r, s, f) -> SecuritySupport.problem(s, factory, mapper, 401))
                .accessDeniedHandler((r, s, f) -> SecuritySupport.problem(s, factory, mapper, 403)))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint((r, s, f) -> SecuritySupport.problem(s, factory, mapper, 401))
                .accessDeniedHandler((r, s, f) -> SecuritySupport.problem(s, factory, mapper, 403))
                .jwt(j -> j.decoder(io.todorok.web.security.TemplateServiceTokens.decoder(env))))
            .authorizeHttpRequests(a -> a.requestMatchers(org.springframework.http.HttpMethod.POST,
                "/internal/template-selections").authenticated().anyRequest().denyAll());
        return http.build();
    }
    @Bean JwtDecoder jwtDecoder(Environment env) { return SecuritySupport.decoder(env); }
    @org.springframework.core.annotation.Order(2)
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            ProblemResponseFactory factory, JsonMapper mapper) throws Exception {
        return SecuritySupport.resourceChain(http, decoder, factory, mapper);
    }
}
