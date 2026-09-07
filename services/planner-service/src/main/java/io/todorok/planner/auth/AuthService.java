package io.todorok.planner.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.todorok.planner.api.model.SessionResponse;
import io.todorok.web.ApiFailure;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthService {
    private final UserAccount users;
    private final RefreshSession sessions;
    private final PasswordEncoder encoder;
    private final RSASSASigner signer;
    private final String issuer;
    private final String audience;
    private final String dummyHash;

    public AuthService(UserAccount users, RefreshSession sessions, PasswordEncoder encoder,
            java.security.interfaces.RSAPrivateKey key, Environment env) {
        this.users = users; this.sessions = sessions; this.encoder = encoder;
        this.signer = new RSASSASigner(key);
        this.issuer = env.getRequiredProperty("todorok.auth.issuer");
        this.audience = env.getRequiredProperty("todorok.auth.audience");
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    public RefreshSession.Issued login(String email, String password) {
        var account = users.find(email);
        boolean matches = password != null && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
                && encoder.matches(password, account.map(UserAccount.Account::passwordHash).orElse(dummyHash));
        if (!matches || account.isEmpty()) throw unauthorized();
        return sessions.create(account.get().id());
    }

    public RefreshSession.Issued refresh(String token) {
        var issued = sessions.rotate(token);
        if (issued == null) throw unauthorized();
        return issued;
    }

    public SessionResponse access(RefreshSession.Issued session) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(600);
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audience).subject(session.userId().toString())
                .issueTime(Date.from(now)).expirationTime(Date.from(expires)).build());
        try { jwt.sign(signer); }
        catch (JOSEException e) { throw new IllegalStateException("Access token signing failed", e); }
        return new SessionResponse(jwt.serialize(), expires.atOffset(ZoneOffset.UTC), session.userId());
    }

    private static ApiFailure unauthorized() {
        return new ApiFailure(401, "UNAUTHORIZED", "Unauthorized", "The credentials or session are invalid.", false);
    }
}
