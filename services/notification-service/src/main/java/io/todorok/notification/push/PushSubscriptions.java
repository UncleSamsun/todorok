package io.todorok.notification.push;

import io.todorok.web.ApiFailure;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushSubscriptions {
    private static final Set<String> HOSTS=Set.of("fcm.googleapis.com","updates.push.services.mozilla.com","web.push.apple.com");
    private final JdbcTemplate jdbc;
    private final PushSecrets secrets;
    public PushSubscriptions(JdbcTemplate jdbc,PushSecrets secrets){this.jdbc=jdbc;this.secrets=secrets;}
    public static void validate(PushSecrets.Secret secret) {
        try {
            URI uri=URI.create(secret.endpoint());
            if(secret.endpoint().length()>4096 || !"https".equals(uri.getScheme()) || uri.getHost()==null
                || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)) || uri.getPort()!=-1
                || uri.getUserInfo()!=null || uri.getFragment()!=null || uri.getRawPath()==null || uri.getRawPath().isEmpty()) throw invalid();
            byte[] point=Base64.getUrlDecoder().decode(secret.p256dh());
            if(point.length!=65 || point[0]!=4 || Base64.getUrlDecoder().decode(secret.auth()).length!=16) throw invalid();
            if(Security.getProvider("BC")==null) Security.addProvider(new BouncyCastleProvider());
            // Validate the curve point, not only its encoded length.
            nl.martijndwars.webpush.Utils.loadPublicKey(secret.p256dh());
        } catch (Exception e) { throw invalid(); }
    }
    private static ApiFailure invalid(){return new ApiFailure(400,"INVALID_PUSH_SUBSCRIPTION","Invalid subscription","A supported HTTPS push endpoint and valid subscription keys are required.",false);}
    public static String hash(String endpoint) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException("SHA-256 unavailable");}
    }
    @Transactional
    public UUID save(UUID owner,PushSecrets.Secret secret) {
        secrets.requireEnabled();validate(secret);
        String hash=hash(secret.endpoint());
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","push-owner:"+owner);
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","push-endpoint:"+hash);
        var existing=jdbc.query("select id,user_id from push_subscription where endpoint_hash=? for update",(row,i)->new UUID[]{row.getObject(1,UUID.class),row.getObject(2,UUID.class)},hash);
        if(!existing.isEmpty() && !existing.getFirst()[1].equals(owner)) throw new ApiFailure(409,"PUSH_OWNERSHIP_CONFLICT","Subscription conflict","Disconnect the previous account's browser subscription and create a new subscription.",false);
        if(jdbc.queryForObject("select count(*) from push_subscription where user_id=? and active and endpoint_hash<>?",Integer.class,owner,hash)>=10) throw new ApiFailure(409,"PUSH_SUBSCRIPTION_LIMIT","Subscription limit","At most ten active subscriptions are allowed.",false);
        UUID id=existing.isEmpty()?UUID.randomUUID():existing.getFirst()[0];
        jdbc.update("""
            insert into push_subscription(id,user_id,endpoint_hash,secret) values (?,?,?,?)
            on conflict(endpoint_hash) do update set secret=excluded.secret,active=true,updated_at=now()
            """,id,owner,hash,secrets.encrypt(owner,hash,secret));
        return id;
    }
    @Transactional public void delete(UUID owner,String endpoint) {
        jdbc.update("update push_subscription set active=false,secret=null,updated_at=now() where user_id=? and endpoint_hash=?",owner,hash(endpoint));
    }
}
