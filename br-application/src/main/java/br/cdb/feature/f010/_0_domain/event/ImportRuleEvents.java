package br.cdb.feature.f010._0_domain.event;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface ImportRuleEvents extends BusinessEvent {

    @NullMarked
    record Created(ImportRule rule) implements ImportRuleEvents {}

    @NullMarked
    record Updated(ImportRule rule) implements ImportRuleEvents {}

    @NullMarked
    record Deleted(UUID ruleId, UUID personId) implements ImportRuleEvents {}
}
