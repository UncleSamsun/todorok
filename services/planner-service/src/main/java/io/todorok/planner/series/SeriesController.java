package io.todorok.planner.series;

import io.todorok.planner.api.SeriesApi;
import io.todorok.planner.api.model.*;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class SeriesController implements SeriesApi {

    private final SeriesService series;

    public SeriesController(SeriesService series) {
        this.series = series;
    }

    private UUID owner() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public ResponseEntity<SeriesResponse> createSeries(CreateSeriesRequest body) {
        return ResponseEntity.status(201).body(series.create(owner(), body));
    }

    public ResponseEntity<SeriesResponse> getSeries(UUID id) {
        return ResponseEntity.ok(series.detail(owner(), id));
    }

    public ResponseEntity<SeriesResponse> updateSeries(UUID id, UpdateSeriesRequest body) {
        return ResponseEntity.ok(series.update(owner(), id, body));
    }

    public ResponseEntity<SeriesResponse> archiveSeries(UUID id, VersionCommand body) {
        return ResponseEntity.ok(series.archive(owner(), id, body.getVersion()));
    }
}
