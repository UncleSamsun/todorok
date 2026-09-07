package io.todorok.planner.template;

import io.todorok.internal.api.model.TemplateSelectionRequest;
import io.todorok.planner.api.model.*;
import io.todorok.web.ApiFailure;
import io.todorok.web.security.TemplateServiceTokens;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TemplateSelectionClient {
    private final Environment env;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public TemplateSelectionClient(Environment env, ObjectMapper mapper) { this.env=env; this.mapper=mapper; }

    public TemplateLink approve(UUID owner, UUID command, String targetType, UUID target, TaskType taskType, TemplateSelection selection) {
        try {
            var request = new TemplateSelectionRequest(command, owner, TemplateSelectionRequest.TargetTypeEnum.valueOf(targetType), target,
                TemplateSelectionRequest.TaskTypeEnum.valueOf(taskType.name()), selection.getTemplateId(), selection.getExpectedTemplateVersion());
            String body = mapper.writeValueAsString(request);
            String token = TemplateServiceTokens.sign(env, body, Map.of("ownerId", owner.toString(), "requestId", command.toString(),
                "targetType", targetType, "targetId", target.toString()));
            String base = env.getRequiredProperty("todorok.template-service.base-url");
            var response = http.send(HttpRequest.newBuilder(URI.create(base + "/internal/template-selections"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (Set.of(400,404,409).contains(response.statusCode())) {
                var problem = mapper.readTree(response.body());
                String code = problem.path("code").asText();
                if (Set.of("VALIDATION_FAILED","NOT_FOUND","COMMAND_REUSE","TEMPLATE_ARCHIVED","TEMPLATE_VERSION_CONFLICT").contains(code))
                    throw new ApiFailure(response.statusCode(), code, "Selection failed", "The template selection could not be approved.", false);
            }
            if (response.statusCode()!=201) throw unavailable();
            var link = mapper.readValue(response.body(), io.todorok.internal.api.model.TemplateLink.class);
            if (link.getBindingId()==null || !selection.getTemplateId().equals(link.getTemplateId())
                || !selection.getExpectedTemplateVersion().equals(link.getSelectedTemplateVersion())
                || link.getName()==null || link.getName().isBlank() || link.getName().length()>120
                || link.getFieldSummary()==null || link.getFieldSummary().length()>240) throw unavailable();
            return new TemplateLink(link.getBindingId(), link.getTemplateId(), link.getSelectedTemplateVersion(), link.getName(), link.getFieldSummary());
        } catch (ApiFailure failure) { throw failure; }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw unavailable(); }
        catch (Exception failure) { throw unavailable(); }
    }
    private static ApiFailure unavailable() {
        return new ApiFailure(503, "TEMPLATE_SERVICE_UNAVAILABLE", "Template service unavailable", "Retry using the same commandId.", true);
    }
}
