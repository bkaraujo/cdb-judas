package br.cdb.feature.f010._1_application.cache;

import br.cdb.core.cache.SessionScopedCache;
import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f010._0_domain.event.ImportRuleEvents;
import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.service.ImportRuleService;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public class ImportRuleCache {
    private final SessionScopedCache<ImportRule> store = new SessionScopedCache<>(
            ImportRuleLayout.PREFIX,
            personId -> Context.get(ImportRuleService.class).findAll(UUID.fromString(personId)),
            ImportRule::id,
            m -> calculateSize(m),
            ImportRuleLayout::write);

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        store.onLogin(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        store.onLogout(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(ImportRuleEvents.Created e) {
        store.upsert(e.rule().personId().toString(), e.rule());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(ImportRuleEvents.Updated e) {
        store.upsert(e.rule().personId().toString(), e.rule());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(ImportRuleEvents.Deleted e) {
        store.evict(e.personId().toString(), e.ruleId());
        return MessageResult.CONSUMED;
    }

    public void forEach(UUID personId, Consumer<ImportRuleLayout.View> consumer) {
        val view = new ImportRuleLayout.View();
        store.forEach(personId.toString(), seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    public boolean find(UUID personId, UUID id, Consumer<ImportRuleLayout.View> consumer) {
        val view = new ImportRuleLayout.View();
        return store.find(personId.toString(), id, seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    private static long calculateSize(ImportRule rule) {
        long size = ImportRuleLayout.HEADER_SIZE;
        for (val trigger : rule.triggers()) {
            size += 4 + trigger.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (size > ImportRuleLayout.HEADER_SIZE + ImportRuleLayout.MAX_TRIGGERS_BLOB) {
                break;
            }
        }
        return size;
    }
}
