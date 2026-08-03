package br.cdb.feature.f010._0_domain.repository;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface ImportRuleRepository {
    List<ImportRule> findAllByPerson(UUID personId);
    Optional<ImportRule> findByPersonAndId(UUID personId, UUID id);
    Optional<ImportRule> findById(UUID id);
    ImportRule save(ImportRule rule);
    void deleteById(UUID id);
}
