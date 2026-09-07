package io.todorok.planner.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoginRateLimiter {
    private final JdbcTemplate jdbc;
    public LoginRateLimiter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public boolean allow(String email, String remoteAddress) {
        jdbc.update("delete from planner.login_attempt_bucket where window_start < now() - interval '10 minutes'");
        int account = consume("account:" + digest(UserAccount.normalize(email)));
        int address = consume("address:" + digest(remoteAddress));
        return account <= 5 && address <= 30;
    }

    private int consume(String key) {
        return jdbc.queryForObject("""
                insert into planner.login_attempt_bucket(bucket_key, window_start, attempts) values (?, now(), 1)
                on conflict (bucket_key) do update set
                    attempts = case when login_attempt_bucket.window_start <= now() - interval '1 minute'
                        then 1 else login_attempt_bucket.attempts + 1 end,
                    window_start = case when login_attempt_bucket.window_start <= now() - interval '1 minute'
                        then now() else login_attempt_bucket.window_start end
                returning attempts
                """, Integer.class, key);
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
