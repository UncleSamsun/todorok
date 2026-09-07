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
    @Bean JwtDecoder jwtDecoder(Environment env) { return SecuritySupport.decoder(env); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            ProblemResponseFactory factory, JsonMapper mapper) throws Exception {
        return SecuritySupport.resourceChain(http, decoder, factory, mapper);
    }
}
