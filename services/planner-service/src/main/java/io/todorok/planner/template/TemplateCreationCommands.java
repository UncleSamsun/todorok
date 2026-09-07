package io.todorok.planner.template;

import io.todorok.planner.api.model.*;
import io.todorok.web.ApiFailure;
import io.todorok.web.security.TemplateServiceTokens;
import java.util.*;
import java.util.function.BiFunction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
public class TemplateCreationCommands {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final TemplateSelectionClient client;
    private final ObjectMapper mapper;
    public TemplateCreationCommands(JdbcTemplate jdbc, PlatformTransactionManager manager, TemplateSelectionClient client, ObjectMapper mapper) {
        this.jdbc=jdbc; this.tx=new TransactionTemplate(manager); this.client=client; this.mapper=mapper;
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** The HTTP approval runs between two committed local transactions, with no held task or command lock. */
    public <T> T create(UUID owner, UUID command, String kind, TaskType type, TemplateSelection selection, Object request,
            Class<T> resultType, BiFunction<UUID,TemplateLink,T> persist) {
        if (selection==null && command==null) return tx.execute(status -> persist.apply(UUID.randomUUID(), null));
        if (selection!=null && (command==null || type==TaskType.GENERAL)) throw new ApiFailure(400, "VALIDATION_FAILED", "Invalid selection",
            "Template selection requires a commandId and a supported task type.", false);
        String fingerprint = TemplateServiceTokens.hash(kind + "|" + mapper.writeValueAsString(request));
        var pending = tx.execute(status -> {
            lock(owner,command);
            var rows=jdbc.queryForList("select * from planner_creation_command where owner_id=? and command_id=?",owner,command);
            if (!rows.isEmpty()) {
                var row=rows.getFirst();
                if (!fingerprint.equals(row.get("fingerprint")) || !kind.equals(row.get("target_type"))) throw reused();
                return new Pending((UUID)row.get("target_id"), row.get("result")==null ? null : row.get("result").toString());
            }
            UUID target=UUID.randomUUID();
            jdbc.update("insert into planner_creation_command(owner_id,command_id,fingerprint,target_type,target_id) values (?,?,?,?,?)",
                owner,command,fingerprint,kind,target);
            return new Pending(target,null);
        });
        if (pending.result()!=null) return mapper.readValue(pending.result(),resultType);
        TemplateLink link=selection==null ? null : client.approve(owner,command,kind,pending.target(),type,selection);
        return tx.execute(status -> {
            lock(owner,command);
            var row=jdbc.queryForMap("select result::text from planner_creation_command where owner_id=? and command_id=? for update",owner,command);
            if (row.get("result")!=null) return mapper.readValue((String)row.get("result"),resultType);
            T result=persist.apply(pending.target(),link);
            jdbc.update("update planner_creation_command set state='COMPLETE',result=cast(? as jsonb) where owner_id=? and command_id=?",
                mapper.writeValueAsString(result),owner,command);
            return result;
        });
    }
    private void lock(UUID owner,UUID command) {
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))",owner+":creation:"+command);
    }
    private static ApiFailure reused() { return new ApiFailure(409,"COMMAND_REUSE","Command reused","Use the original request for this commandId.",false); }
    private record Pending(UUID target,String result) {}
}
