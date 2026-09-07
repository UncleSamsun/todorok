package io.todorok.web.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;

public final class RsaKeys {
    private RsaKeys() {}

    public static RSAPublicKey publicKey(Environment environment) {
        try {
            var key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(read(environment, "public", "PUBLIC KEY")));
            if (key.getModulus().bitLength() < 2048) throw new IllegalArgumentException("RSA key is too small");
            return key;
        } catch (Exception e) { throw new IllegalStateException("Valid RSA public key configuration is required", e); }
    }

    public static RSAPrivateKey privateKey(Environment environment) {
        try {
            var key = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(read(environment, "private", "PRIVATE KEY")));
            if (key.getModulus().bitLength() < 2048) throw new IllegalArgumentException("RSA key is too small");
            return key;
        } catch (Exception e) { throw new IllegalStateException("Valid PKCS8 RSA private key configuration is required", e); }
    }

    private static byte[] read(Environment environment, String kind, String label) throws Exception {
        String pem = environment.getProperty("todorok.auth." + kind + "-key", "");
        if (pem.isBlank()) {
            String location = environment.getRequiredProperty("todorok.auth." + kind + "-key-location");
            if (location.isBlank()) throw new IllegalArgumentException("Missing RSA key location");
            try (var input = new DefaultResourceLoader().getResource(location).getInputStream()) {
                pem = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
            }
        }
        return Base64.getDecoder().decode(pem.replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "").replaceAll("\\s", ""));
    }
}
