package br.cdb.feature.f010._0_domain.repository;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Porta dos gatilhos de uma regra de nomenclatura ({@code F010_IMPORT_RULE_TRIGGER}) — tabela à
 * parte de {@code F010_IMPORT_RULE}, mesmo molde de {@code TransactionTagRepository} (f006).
 */
@NullMarked
public interface ImportRuleTriggerRepository {
    /** Substitui todos os gatilhos da regra por {@code triggers} (DELETE + INSERT). */
    void replaceTriggers(UUID ruleId, UUID personId, List<String> triggers);

    /** Gatilhos por regra, para todas as regras de {@code personId}. */
    Map<UUID, List<String>> findTriggersByPerson(UUID personId);

    /** Apaga todos os gatilhos da regra (cascata de exclusão). */
    void deleteTriggersByRule(UUID ruleId);
}
