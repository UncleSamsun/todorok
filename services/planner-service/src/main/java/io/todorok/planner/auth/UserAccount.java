package io.todorok.planner.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserAccount {
    private final JdbcTemplate jdbc;
    public UserAccount(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Account(UUID id, String passwordHash) {}

    public Optional<Account> find(String email) {
        return jdbc.query("select id, password_hash from planner.user_account where email = ?",
                (rs, row) -> new Account(rs.getObject("id", UUID.class), rs.getString("password_hash")),
                normalize(email)).stream().findFirst();
    }

    @Transactional
    public UUID bootstrap(String email, String password, PasswordEncoder encoder) {
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 254
                || password == null || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Bootstrap requires a valid email and a 12 to 72-byte password");
        }
        // Serialize first-account creation across simultaneous command invocations.
        jdbc.execute("select pg_advisory_xact_lock(742019202602)");
        var existing = jdbc.queryForList("select id from planner.user_account order by created_at limit 1", UUID.class);
        if (!existing.isEmpty()) return existing.getFirst();
        UUID id = UUID.randomUUID();
        jdbc.update("insert into planner.user_account(id, email, password_hash) values (?, ?, ?)",
                id, normalize(email), encoder.encode(password));
        return id;
    }

    public static String normalize(String email) { return email == null ? "" : email.strip().toLowerCase(Locale.ROOT); }
}
