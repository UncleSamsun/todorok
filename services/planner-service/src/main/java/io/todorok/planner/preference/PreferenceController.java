package io.todorok.planner.preference;

import io.todorok.planner.api.PreferenceApi;
import io.todorok.planner.api.model.ThemeMode;
import io.todorok.planner.api.model.UpdateUserPreferencesRequest;
import io.todorok.planner.api.model.UserPreferencesResponse;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class PreferenceController implements PreferenceApi {
    private final UserPreference preferences;
    public PreferenceController(UserPreference preferences) { this.preferences = preferences; }
    @Override public ResponseEntity<UserPreferencesResponse> getPreferences() { return ResponseEntity.ok(response(preferences.get(owner()))); }
    @Override public ResponseEntity<UserPreferencesResponse> updatePreferences(UpdateUserPreferencesRequest request) {
        return ResponseEntity.ok(response(preferences.save(owner(), request.getTheme().getValue(), request.getNotificationsEnabled(), request.getSummaryTime(), request.getExpectedRevision())));
    }
    private UUID owner() { return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    private UserPreferencesResponse response(UserPreference.Value value) {
        return new UserPreferencesResponse(ThemeMode.fromValue(value.theme()), value.notificationsEnabled(), value.summaryTime(), value.revision());
    }
}
