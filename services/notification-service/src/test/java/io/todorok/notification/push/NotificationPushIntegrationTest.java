package io.todorok.notification.push;

import static org.assertj.core.api.Assertions.assertThat;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.flyway.enabled=true", "spring.flyway.create-schemas=true", "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration", "todorok.push.enabled=true",
    "todorok.push.worker-enabled=false"})
class NotificationPushIntegrationTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:17.11-alpine");
    static final KeyPair KEYS=keys();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",DB::getJdbcUrl);
        r.add("spring.datasource.username",DB::getUsername);
        r.add("spring.datasource.password",DB::getPassword);
        r.add("spring.datasource.hikari.connection-init-sql",()->"set search_path to notification");
        r.add("todorok.auth.public-key",()->Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
        r.add("todorok.push.encryption-key",()->Base64.getEncoder().encodeToString(new byte[32]));
        r.add("todorok.push.public-key",()->"test-public-key");
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.json.JsonMapper mapper;

    @Test void registersEncryptedSubscriptionIdempotentlyWithoutLeakingOrTransferringOwnership() throws Exception {
        var owner=UUID.randomUUID(); var other=UUID.randomUUID();
        String endpoint="https://fcm.googleapis.com/fcm/send/"+UUID.randomUUID();
        String body=subscription(endpoint);
        var first=request(owner,"POST","/push/subscriptions",body);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).doesNotContain(endpoint,"p256dh","auth");
        assertThat(request(owner,"POST","/push/subscriptions",body).body()).isEqualTo(first.body());
        assertThat(request(other,"POST","/push/subscriptions",body).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("select secret from push_subscription where user_id=?",String.class,owner))
            .doesNotContain(endpoint,"p256dh");
        assertThat(request(other,"DELETE","/push/subscriptions","{\"endpoint\":\""+endpoint+"\"}").statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("select active from push_subscription where user_id=?",Boolean.class,owner)).isTrue();
        assertThat(request(owner,"DELETE","/push/subscriptions","{\"endpoint\":\""+endpoint+"\"}").statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("select secret from push_subscription where user_id=?",String.class,owner)).isNull();
    }
    @Test void rejectsMissingAuthPrivateHostsRedirectTargetsPortsAndInvalidKeys() throws Exception {
        assertThat(request(null,"GET","/push/config",null).statusCode()).isEqualTo(401);
        for(String host:List.of("http://fcm.googleapis.com/x","https://127.0.0.1/x","https://fcm.googleapis.com.evil.test/x",
            "https://fcm.googleapis.com:8443/x","https://user@fcm.googleapis.com/x","https://fcm.googleapis.com/x#fragment")) {
            assertThat(request(UUID.randomUUID(),"POST","/push/subscriptions",subscription(host)).statusCode()).isEqualTo(400);
        }
        assertThat(request(UUID.randomUUID(),"POST","/push/subscriptions","{\"endpoint\":\"https://fcm.googleapis.com/x\",\"keys\":{\"p256dh\":\"bad\",\"auth\":\"bad\"}}").statusCode()).isEqualTo(400);
    }
    @Test void limitsActiveSubscriptionsAndScopesDeliveryHistory() throws Exception {
        var owner=UUID.randomUUID();
        for(int i=0;i<10;i++) assertThat(request(owner,"POST","/push/subscriptions",subscription("https://fcm.googleapis.com/"+UUID.randomUUID())).statusCode()).isEqualTo(200);
        assertThat(request(owner,"POST","/push/subscriptions",subscription("https://fcm.googleapis.com/"+UUID.randomUUID())).statusCode()).isEqualTo(409);
        UUID id=UUID.randomUUID();
        jdbc.update("insert into notification_delivery(id,user_id,kind,scheduled_for,deadline_at,status) values (?,?,'DAILY_SUMMARY',now(),now()+interval '1 hour','FAILED')",id,owner);
        assertThat(request(owner,"GET","/deliveries",null).body()).contains(id.toString());
        assertThat(request(UUID.randomUUID(),"GET","/deliveries",null).body()).doesNotContain(id.toString());
    }
    HttpResponse<String> request(UUID owner,String method,String path,String body)throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/notification/v1"+path));
        if(owner!=null) r.header("Authorization","Bearer "+token(owner));
        if(body!=null) r.header("Content-Type","application/json");
        return HttpClient.newHttpClient().send(r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    static String subscription(String endpoint) throws Exception {
        var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        var key=(java.security.interfaces.ECPublicKey)generator.generateKeyPair().getPublic();
        byte[] point=new byte[65];point[0]=4;
        coordinate(key.getW().getAffineX().toByteArray(),point,1);coordinate(key.getW().getAffineY().toByteArray(),point,33);
        return "{\"endpoint\":\""+endpoint+"\",\"keys\":{\"p256dh\":\""+Base64.getUrlEncoder().withoutPadding().encodeToString(point)+"\",\"auth\":\"AAAAAAAAAAAAAAAAAAAAAA\"}}";
    }
    static void coordinate(byte[] from,byte[] to,int offset){int length=Math.min(from.length,32);System.arraycopy(from,from.length-length,to,offset+32-length,length);}
    static KeyPair keys(){try{var g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);return g.generateKeyPair();}catch(Exception e){throw new IllegalStateException(e);}}
    static String token(UUID owner)throws Exception {
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),new JWTClaimsSet.Builder().issuer("todorok").audience("todorok-api").subject(owner.toString()).issueTime(new Date()).expirationTime(Date.from(java.time.Instant.now().plusSeconds(300))).build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey)KEYS.getPrivate()));return jwt.serialize();
    }
}
