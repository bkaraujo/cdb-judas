package br.cdb.feature.f010._1_application.cache;

import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f010._0_domain.event.ImportRuleEvents;
import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.service.ImportRuleService;
import br.commons.cache.AbstractCache;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.UUID;

@NullMarked
public class ImportRuleCache extends AbstractCache<ImportRule> {

    public ImportRuleCache() {
        super(
                ImportRuleLayout.PREFIX,
                personId -> {
                    val service = Context.tryGet(ImportRuleService.class);
                    return service.findAll(UUID.fromString(personId));
                },
                ImportRule::id,
                ImportRuleCache::calculateSize,
                ImportRuleLayout::write
        );
    }

    @Override
    protected ImportRule mapToDomain(MemorySegment segment) {
        val view = new ImportRuleLayout.View().bind(segment);
        val triggers = new ArrayList<String>();
        val triggerCount = view.triggerCount();
        for (int i = 0; i < triggerCount; i++) {
            val trigger = view.trigger(i);
            if (trigger != null) triggers.add(trigger);
        }
        return new ImportRule(
                view.id(), view.personId(), view.name(), triggers,
                view.accountId(), view.categoryId(), view.planned(), view.createdAt()
        );
    }

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        onLogin(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        onLogout(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(ImportRuleEvents.Created e) {
        upsert(e.rule().personId().toString(), e.rule());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(ImportRuleEvents.Updated e) {
        upsert(e.rule().personId().toString(), e.rule());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(ImportRuleEvents.Deleted e) {
        evict(e.personId().toString(), e.ruleId());
        return MessageResult.CONSUMED;
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

