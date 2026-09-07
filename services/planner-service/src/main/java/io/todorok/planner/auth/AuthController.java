package io.todorok.planner.auth;

import io.todorok.planner.api.AuthApi;
import io.todorok.planner.api.model.LoginRequest;
import io.todorok.planner.api.model.SessionResponse;
import io.todorok.web.ApiFailure;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class AuthController implements AuthApi {
    public static final String COOKIE_NAME = "todorok_refresh";
    public static final String COOKIE_PATH = "/api/planner/v1/auth";
    private final AuthService auth;
    private final RefreshSession sessions;
    private final LoginRateLimiter limiter;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final boolean secure;

    public AuthController(AuthService auth, RefreshSession sessions, LoginRateLimiter limiter,
            HttpServletRequest request, HttpServletResponse response, Environment env) {
        this.auth = auth; this.sessions = sessions; this.limiter = limiter;
        this.request = request; this.response = response;
        secure = env.getProperty("todorok.auth.cookie-secure", Boolean.class, true);
        if (!secure && !env.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException("Insecure cookies are permitted only in the local profile");
        }
    }

    @Override public ResponseEntity<SessionResponse> login(LoginRequest body) {
        if (!limiter.allow(body.getEmail(), request.getRemoteAddr())) {
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");
            throw new ApiFailure(429, "RATE_LIMITED", "Too many requests", "Try signing in again later.", true);
        }
        return session(auth.login(body.getEmail(), body.getPassword()));
    }

    @Override public ResponseEntity<SessionResponse> refreshSession() {
        try { return session(auth.refresh(cookie())); }
        catch (ApiFailure failure) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieHeader("", Duration.ZERO));
            throw failure;
        }
    }

    @Override public ResponseEntity<Void> logout() {
        UUID user = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        sessions.logout(cookie(), user);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookieHeader("", Duration.ZERO))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    private ResponseEntity<SessionResponse> session(RefreshSession.Issued issued) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookieHeader(issued.token(), Duration.ofDays(30)))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(auth.access(issued));
    }

    private String cookie() {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        var matches = Arrays.stream(cookies).filter(c -> c.getName().equals(COOKIE_NAME)).toList();
        return matches.size() == 1 ? matches.getFirst().getValue() : null;
    }

    private String cookieHeader(String value, Duration age) {
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true).secure(secure).sameSite("Lax")
                .path(COOKIE_PATH).maxAge(age).build().toString();
    }
}
