package io.todorok.activity.template;

import io.todorok.internal.api.TemplateSelectionApi;
import io.todorok.internal.api.model.*;
import io.todorok.web.ApiFailure;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
public class TemplateSelectionController implements TemplateSelectionApi {
    private final TemplateBindingService bindings;
    public TemplateSelectionController(TemplateBindingService bindings) { this.bindings = bindings; }

    @Override public ResponseEntity<TemplateLink> approveTemplateSelection(TemplateSelectionRequest request) {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!request.getOwnerId().toString().equals(jwt.getClaimAsString("ownerId"))
            || !request.getRequestId().toString().equals(jwt.getClaimAsString("requestId"))
            || !request.getTargetId().toString().equals(jwt.getClaimAsString("targetId"))
            || !request.getTargetType().name().equals(jwt.getClaimAsString("targetType")))
            throw new ApiFailure(403, "FORBIDDEN", "Forbidden", "Request claims do not match.", false);
        return ResponseEntity.status(201).body(bindings.approve(request));
    }
}
