package io.todorok.notification.push;

import io.todorok.notification.api.PushApi;
import io.todorok.notification.api.model.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
@RequestMapping("/api/notification/v1")
public class PushController implements PushApi {
    private final PushSubscriptions subscriptions;
    private final PushSecrets secrets;
    private final JdbcTemplate jdbc;
    public PushController(PushSubscriptions subscriptions,PushSecrets secrets,JdbcTemplate jdbc){this.subscriptions=subscriptions;this.secrets=secrets;this.jdbc=jdbc;}
    private UUID owner(){return (UUID)SecurityContextHolder.getContext().getAuthentication().getPrincipal();}
    @Override public ResponseEntity<GetPushConfig200Response> getPushConfig(){return ResponseEntity.ok(new GetPushConfig200Response(secrets.publicKey()));}
    @Override public ResponseEntity<SavePushSubscription200Response> savePushSubscription(PushSubscriptionRequest request){
        var keys=request.getKeys();
        return ResponseEntity.ok(new SavePushSubscription200Response(subscriptions.save(owner(),new PushSecrets.Secret(request.getEndpoint(),keys.getP256dh(),keys.getAuth()))));
    }
    @Override public ResponseEntity<Void> deletePushSubscription(DeletePushSubscriptionRequest request){subscriptions.delete(owner(),request.getEndpoint());return ResponseEntity.noContent().build();}
    @Override public ResponseEntity<List<ListNotificationDeliveries200ResponseInner>> listNotificationDeliveries(){
        return ResponseEntity.ok(jdbc.query("select id,status,attempts,scheduled_for,error_code from notification_delivery where user_id=? order by scheduled_for desc,id limit 50",(row,i)->new ListNotificationDeliveries200ResponseInner(row.getObject(1,UUID.class),ListNotificationDeliveries200ResponseInner.StatusEnum.fromValue(row.getString(2)),row.getInt(3),row.getObject(4,OffsetDateTime.class)).errorCode(row.getString(5)),owner()));
    }
}
