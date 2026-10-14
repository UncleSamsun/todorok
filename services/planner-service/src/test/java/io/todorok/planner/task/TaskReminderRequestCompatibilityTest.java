package io.todorok.planner.task;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import io.todorok.planner.api.model.CreateTaskRequest;
import tools.jackson.databind.json.JsonMapper;

class TaskReminderRequestCompatibilityTest {
    @Test void omittedReminderFieldsPreserveThePreviousCommandFingerprint(){
        var mapper=JsonMapper.builder().findAndAddModules().build();
        String legacy="{\"commandId\":\"00000000-0000-0000-0000-000000000001\",\"title\":\"기존 요청\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-10-13\"}";
        var request=mapper.readValue(legacy,CreateTaskRequest.class);
        var serialized=mapper.writeValueAsString(request);
        assertThat(mapper.readTree(serialized).has("reminderEnabled")).isFalse();
        assertThat(serialized).isEqualTo("{\"title\":\"기존 요청\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-10-13\",\"commandId\":\"00000000-0000-0000-0000-000000000001\"}");
    }
}
