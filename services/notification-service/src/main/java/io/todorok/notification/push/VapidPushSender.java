package io.todorok.notification.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.stereotype.Component;

@Component
public class VapidPushSender implements PushSender {
    private final PushSecrets secrets;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    public VapidPushSender(PushSecrets secrets){this.secrets=secrets;}
    @Override public int send(PushSecrets.Secret subscription,UUID deliveryId,Instant deadline) {
        try {
            PushSubscriptions.validate(subscription);
            long seconds=Duration.between(Instant.now(),deadline).toSeconds();
            if(seconds<=0)return 408;
            var service=new RequestPreparer(secrets.value("public-key"),secrets.value("private-key"),secrets.value("subject"));
            var notification=new Notification(subscription.endpoint(),subscription.p256dh(),subscription.auth(),
                ("{\"id\":\""+deliveryId+"\"}").getBytes(StandardCharsets.UTF_8),(int)Math.min(seconds,86400));
            var prepared=service.prepare(notification);
            var request=HttpRequest.newBuilder(URI.create(prepared.getUrl()))
                .timeout(Duration.ofSeconds(Math.min(seconds,5)))
                .POST(HttpRequest.BodyPublishers.ofByteArray(prepared.getBody()));
            prepared.getHeaders().forEach(request::header);
            return http.send(request.build(),HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch(InterruptedException e){Thread.currentThread().interrupt();return 0;}
        catch(Exception e){return 0;}
    }
    private static final class RequestPreparer extends PushService {
        RequestPreparer(String publicKey,String privateKey,String subject) throws java.security.GeneralSecurityException {
            super(publicKey,privateKey,subject);
        }
        nl.martijndwars.webpush.HttpRequest prepare(Notification notification)
            throws java.security.GeneralSecurityException,java.io.IOException,org.jose4j.lang.JoseException {
            return prepareRequest(notification,Encoding.AES128GCM);
        }
    }
}
