package io.todorok.planner.note;

import io.todorok.planner.api.model.DailyNoteResponse;
import java.time.LocalDate;

public record DailyNote(LocalDate date, String content, Long version) {
    DailyNoteResponse response() {
        return new DailyNoteResponse(date, content, version);
    }
}
