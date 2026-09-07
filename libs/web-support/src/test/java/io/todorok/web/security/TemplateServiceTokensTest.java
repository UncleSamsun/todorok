package io.todorok.web.security;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class TemplateServiceTokensTest {
    @TempDir Path directory;

    @Test
    void separatePemFilesSignAndVerifyBoundServiceClaims() throws Exception {
        var user=keys(); var service=keys();
        var env=new MockEnvironment().withProperty("todorok.auth.public-key",Base64.getEncoder().encodeToString(user.getPublic().getEncoded()));
        Path privateFile=directory.resolve("private.pem"), publicFile=directory.resolve("public.pem");
        Files.writeString(privateFile,pem("PRIVATE KEY",service.getPrivate().getEncoded()));
        Files.writeString(publicFile,pem("PUBLIC KEY",service.getPublic().getEncoded()));
        env.withProperty("todorok.template-service.private-key-location",privateFile.toUri().toString());
        env.withProperty("todorok.template-service.public-key-location",publicFile.toUri().toString());
        String body="{\"targetType\":\"TASK\"}";
        var jwt=TemplateServiceTokens.decoder(env).decode(TemplateServiceTokens.sign(env,body,Map.of("targetType","TASK")));
        assertThat(jwt.getSubject()).isEqualTo("todorok-planner");
        assertThat(jwt.getClaimAsString("fingerprint")).isEqualTo("d44158848e667f830f80800517e494be765517969d25ee21ed934becd3cbc499");
        assertThat(jwt.getClaimAsString("targetType")).isEqualTo("TASK");
    }

    @Test
    void missingServiceConfigurationAndReusedUserKeysFailClosed() throws Exception {
        var user=keys();
        var env=new MockEnvironment().withProperty("todorok.auth.public-key",Base64.getEncoder().encodeToString(user.getPublic().getEncoded()));
        assertThatThrownBy(()->TemplateServiceTokens.decoder(env).decode("untrusted")).isInstanceOf(org.springframework.security.oauth2.jwt.BadJwtException.class);
        assertThatThrownBy(()->TemplateServiceTokens.sign(env,"{}",Map.of())).isInstanceOf(IllegalStateException.class);
        env.withProperty("todorok.template-service.public-key",Base64.getEncoder().encodeToString(user.getPublic().getEncoded()));
        env.withProperty("todorok.template-service.private-key",Base64.getEncoder().encodeToString(user.getPrivate().getEncoded()));
        assertThatThrownBy(()->TemplateServiceTokens.decoder(env)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->TemplateServiceTokens.sign(env,"{}",Map.of())).isInstanceOf(IllegalStateException.class);
    }

    private static KeyPair keys() throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair();
    }
    private static String pem(String label,byte[] value) {
        return "-----BEGIN "+label+"-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(value)+"\n-----END "+label+"-----\n";
    }
}
