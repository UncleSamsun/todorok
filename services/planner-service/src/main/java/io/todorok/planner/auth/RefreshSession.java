package io.todorok.planner.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefreshSession {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    public RefreshSession(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Issued(UUID userId, String token, Instant expiresAt) {}
    private record Stored(String hash, UUID familyId, UUID userId, boolean consumed, boolean expired, boolean revoked) {}

    @Transactional
    public Issued create(UUID userId) {
        UUID family = UUID.randomUUID();
        jdbc.update("insert into planner.refresh_family(id, user_id) values (?, ?)", family, userId);
        return issue(family, userId);
    }

    @Transactional
    public Issued rotate(String token) {
        Stored stored = lock(token);
        if (stored == null || stored.revoked()) return null;
        if (stored.consumed()) {
            revoke(stored.familyId());
            // Return failure normally: throwing here would roll back replay revocation.
            return null;
        }
        if (stored.expired()) return null;
        jdbc.update("update planner.refresh_session set consumed_at = now() where token_hash = ?", stored.hash());
        return issue(stored.familyId(), stored.userId());
    }

    @Transactional
    public void logout(String token, UUID userId) {
        Stored stored = lock(token);
        if (stored != null && stored.userId().equals(userId)) revoke(stored.familyId());
    }

    private Stored lock(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        String hash = hash(token);
        var family = jdbc.queryForList("select family_id from planner.refresh_session where token_hash = ?", UUID.class, hash);
        if (family.isEmpty()) return null;
        // Always lock the family before reading token state. Under READ COMMITTED, the next statement
        // sees the winner's consumed_at even when this request waited for another rotation to commit.
        jdbc.queryForObject("select id from planner.refresh_family where id = ? for update", UUID.class, family.getFirst());
        return jdbc.queryForObject("""
                select s.token_hash, s.family_id, f.user_id, s.consumed_at is not null as consumed,
                    s.expires_at <= now() as expired, f.revoked_at is not null as revoked
                from planner.refresh_session s join planner.refresh_family f on f.id = s.family_id
                where s.token_hash = ?
                """, (rs, row) -> new Stored(rs.getString("token_hash"), rs.getObject("family_id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getBoolean("consumed"), rs.getBoolean("expired"),
                rs.getBoolean("revoked")), hash);
    }

    private Issued issue(UUID family, UUID user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(30, ChronoUnit.DAYS);
        jdbc.update("insert into planner.refresh_session(token_hash, family_id, expires_at) values (?, ?, ?)",
                hash(token), family, Timestamp.from(expires));
        return new Issued(user, token, expires);
    }

    private void revoke(UUID family) {
        jdbc.update("update planner.refresh_family set revoked_at = coalesce(revoked_at, now()) where id = ?", family);
    }

    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
