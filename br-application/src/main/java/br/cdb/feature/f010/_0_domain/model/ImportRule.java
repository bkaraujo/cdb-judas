package br.cdb.feature.f010._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NullMarked
public record ImportRule(
        UUID id,
        UUID personId,
        String name,
        List<String> triggers,
        @Nullable UUID accountId,
        @Nullable UUID categoryId,
        @Nullable Boolean planned,
        @Nullable LocalDateTime createdAt
) {
    /** Reconstrói a regra com outra lista de gatilhos (imutável, o record em si não muda). */
    public ImportRule withTriggers(List<String> triggers) {
        return new ImportRule(id, personId, name, triggers, accountId, categoryId, planned, createdAt);
    }
}
