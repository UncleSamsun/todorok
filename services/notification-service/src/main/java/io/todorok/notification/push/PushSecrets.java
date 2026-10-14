package io.todorok.notification.push;

import io.todorok.web.ApiFailure;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PushSecrets {
    private final Environment env;
    private final ResourceLoader resources;
    private final JsonMapper mapper;
    private final SecureRandom random = new SecureRandom();
    public PushSecrets(Environment env, ResourceLoader resources, JsonMapper mapper) {
        this.env=env; this.resources=resources; this.mapper=mapper;
    }
    public String value(String name) {
        String location=env.getProperty("todorok.push."+name+"-location", "");
        if (location.isBlank()) return env.getProperty("todorok.push."+name, "").trim();
        try (var input=resources.getResource(location).getInputStream()) {
            return new String(input.readNBytes(4096),StandardCharsets.UTF_8).trim();
        } catch (Exception e) { throw unavailable(); }
    }
    public String publicKey() {
        return env.getProperty("todorok.push.enabled",Boolean.class,false) ? value("public-key") : "";
    }
    public void requireEnabled() {
        if (publicKey().isBlank()) throw unavailable();
        key();
    }
    private SecretKeySpec key() {
        try {
            byte[] bytes=Base64.getDecoder().decode(value("encryption-key"));
            if(bytes.length!=32) throw unavailable();
            return new SecretKeySpec(bytes,"AES");
        } catch (IllegalArgumentException e) { throw unavailable(); }
    }
    public String encrypt(UUID owner, String hash, Secret secret) {
        try {
            byte[] nonce=new byte[12];random.nextBytes(nonce);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,nonce));
            cipher.updateAAD((owner+":"+hash).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted=cipher.doFinal(mapper.writeValueAsBytes(secret));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length+encrypted.length).put(nonce).put(encrypted).array());
        } catch (Exception e) { throw unavailable(); }
    }
    public Secret decrypt(UUID owner,String hash,String encoded) {
        try {
            var buffer=ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
            byte[] nonce=new byte[12];buffer.get(nonce);
            byte[] encrypted=new byte[buffer.remaining()];buffer.get(encrypted);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,nonce));
            cipher.updateAAD((owner+":"+hash).getBytes(StandardCharsets.UTF_8));
            return mapper.readValue(cipher.doFinal(encrypted),Secret.class);
        } catch (Exception e) { throw unavailable(); }
    }
    private static ApiFailure unavailable() {
        return new ApiFailure(503,"PUSH_UNAVAILABLE","Push unavailable","Push is not configured or its secret cannot be read.",true);
    }
    public record Secret(String endpoint,String p256dh,String auth) {
        @Override public String toString(){return "PushSecret[redacted]";}
    }
}
