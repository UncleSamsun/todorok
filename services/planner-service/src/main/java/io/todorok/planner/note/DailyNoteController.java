package io.todorok.planner.note;

import io.todorok.planner.api.NoteApi;
import io.todorok.planner.api.model.DailyNoteResponse;
import io.todorok.planner.api.model.UpdateDailyNoteRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class DailyNoteController implements NoteApi {

    private final DailyNoteRepository notes;

    public DailyNoteController(DailyNoteRepository notes) {
        this.notes = notes;
    }

    private UUID owner() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public ResponseEntity<DailyNoteResponse> getDailyNote(LocalDate date) {
        return ResponseEntity.ok(notes.get(owner(), date).response());
    }

    @Override
    public ResponseEntity<DailyNoteResponse> updateDailyNote(
        LocalDate date,
        UpdateDailyNoteRequest body
    ) {
        return ResponseEntity.ok(
            notes.save(owner(), date, body.getContent(), body.getExpectedVersion()).response()
        );
    }
}
