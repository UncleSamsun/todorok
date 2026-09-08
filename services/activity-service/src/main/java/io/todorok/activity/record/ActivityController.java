package io.todorok.activity.record;

import io.todorok.activity.api.ActivityApi;
import io.todorok.activity.api.model.*;
import io.todorok.activity.program.ProgramCatalogStore;
import io.todorok.activity.program.ProgramEnrollmentService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import io.todorok.web.ApiFailure;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class ActivityController implements ActivityApi {

    private final ActivityService activities;
    private final ProgramCatalogStore catalogs;
    private final ProgramEnrollmentService enrollments;

    public ActivityController(ActivityService activities, ProgramCatalogStore catalogs, ProgramEnrollmentService enrollments) {
        this.activities = activities;
        this.catalogs = catalogs;
        this.enrollments = enrollments;
    }

    private UUID owner() {
        return (UUID) SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
    }

    @Override
    public ResponseEntity<ActivityResponse> createActivity(
        CreateActivityRequest request
    ) {
        return ResponseEntity.status(201).body(
            activities.create(owner(), request)
        );
    }

    @Override
    public ResponseEntity<ActivityResponse> correctActivity(UUID id, CorrectActivityRequest request) {
        return ResponseEntity.ok(activities.correct(owner(), id, request));
    }

    @Override
    public ResponseEntity<ActivityResponse> getActivity(UUID id) {
        return ResponseEntity.ok(activities.get(owner(), id));
    }

    @Override
    public ResponseEntity<MonthlyActivitySummaryResponse> getMonthlyActivitySummary(
        String month,
        ActivityType activityType
    ) {
        try {
            return ResponseEntity.ok(activities.monthlySummary(owner(), YearMonth.parse(month), activityType));
        } catch (DateTimeParseException invalidMonth) {
            throw new ApiFailure(400, "INVALID_MONTH", "Invalid month", "Use YYYY-MM.", false);
        }
    }

    @Override
    public ResponseEntity<ActivityPageResponse> listActivities(
        LocalDate date,
        String cursor,
        Integer limit
    ) {
        return ResponseEntity.ok(activities.list(owner(), date, cursor, limit));
    }

    @Override
    public ResponseEntity<ActivityResponse> voidActivity(
        UUID id,
        VoidActivityRequest request
    ) {
        return ResponseEntity.ok(activities.voidRecord(owner(), id, request));
    }

    @Override
    public ResponseEntity<java.util.List<ProgramCatalogSummary>> listPrograms() {
        return ResponseEntity.ok(catalogs.list().stream().map(value -> new ProgramCatalogSummary(value.catalogKey(), value.catalogVersion(), value.checksum(), value.name(), value.sessionsPerWeek(), value.totalWeeks())).toList());
    }

    @Override
    public ResponseEntity<ProgramEnrollmentResponse> enrollProgram(EnrollProgramRequest request) {
        return ResponseEntity.status(201).body(response(enrollments.enroll(owner(), request.getCommandId(), request.getCatalogKey(), request.getCatalogVersion(), request.getInitialTestValue(), request.getStartWeek())));
    }

    @Override
    public ResponseEntity<ProgramEnrollmentResponse> getProgramEnrollment(UUID enrollmentId) {
        return ResponseEntity.ok(response(enrollments.get(owner(), enrollmentId)));
    }

    private ProgramEnrollmentResponse response(ProgramEnrollmentService.Enrollment value) {
        return new ProgramEnrollmentResponse(value.id(), value.catalogKey(), value.catalogVersion(), value.recommendedWeek(), value.startWeek(), value.currentWeek(), value.currentSession(), ProgramEnrollmentResponse.StatusEnum.ACTIVE,
            new ProgramSessionTarget(value.sessionId(), value.currentWeek(), value.currentSession(), value.targetSets()));
    }
}
