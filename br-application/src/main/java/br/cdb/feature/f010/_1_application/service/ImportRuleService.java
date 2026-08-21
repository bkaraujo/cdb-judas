package br.cdb.feature.f010._1_application.service;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.cdb.feature.f010._0_domain.repository.ImportRuleTriggerRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Context-wired como os demais serviços de fatia (o par {@code ReadUseCase}/{@code WriteUseCase} o
 *  resolve com {@code Context.tryGet}); sem CDI. */
@NullMarked
public class ImportRuleService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private final ImportRuleRepository repository = Context.get(ImportRuleRepository.class);
    private final ImportRuleTriggerRepository triggerRepository = Context.get(ImportRuleTriggerRepository.class);

    /** Uppercase + remove acentos, pra casar "Água" com "AGUA" na comparação de gatilhos. */
    public static String normalize(String s) {
        val decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toUpperCase(Locale.ROOT).trim();
    }

    /** Trim + descarta vazios + dedupe (preserva ordem) — aplicado antes de validar/gravar. */
    private static List<String> sanitizeTriggers(List<String> triggers) {
        return triggers.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();
    }

    public List<ImportRule> findAll(UUID personId) {
        val rules = repository.findAllByPerson(personId);
        val triggersByRule = triggerRepository.findTriggersByPerson(personId);
        return rules.stream()
                .map(rule -> rule.withTriggers(triggersByRule.getOrDefault(rule.id(), List.of())))
                .toList();
    }

    public Result<ImportRule, BusinessError> find(UUID personId, UUID id) {
        return repository.findByPersonAndId(personId, id)
                .map(rule -> rule.withTriggers(
                        triggerRepository.findTriggersByPerson(personId).getOrDefault(rule.id(), List.of())))
                .<Result<ImportRule, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("importRule.notFound", id)));
    }

    public Result<ImportRule, BusinessError> create(
            UUID personId, String name, List<String> triggers,
            @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        val sanitized = sanitizeTriggers(triggers);
        val conflict = findAmbiguousConflict(personId, sanitized, null);
        if (conflict != null) return Result.failure(conflict);

        val saved = repository.save(new ImportRule(
                UUID.randomUUID(), personId, name, List.of(), accountId, categoryId, costCenterId, null));
        triggerRepository.replaceTriggers(saved.id(), personId, sanitized);
        return Result.success(saved.withTriggers(sanitized));
    }

    public Result<ImportRule, BusinessError> update(
            UUID personId, UUID id, String name, List<String> triggers,
            @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        return find(personId, id).flatMap(existing -> {
            val sanitized = sanitizeTriggers(triggers);
            val conflict = findAmbiguousConflict(personId, sanitized, id);
            if (conflict != null) return Result.failure(conflict);

            val saved = repository.save(new ImportRule(
                    id, existing.personId(), name, List.of(), accountId, categoryId, costCenterId, existing.createdAt()));
            triggerRepository.replaceTriggers(id, personId, sanitized);
            return Result.success(saved.withTriggers(sanitized));
        });
    }

    /** Sem estratégia e sem vínculos na regra em si: exclusão simples; gatilhos são cascata. */
    public Result<Void, BusinessError> deleteById(UUID id) {
        triggerRepository.deleteTriggersByRule(id);
        repository.deleteById(id);
        return Result.success();
    }

    /** Gatilhos ambíguos (um é substring do outro, em qualquer direção) entre regras DIFERENTES da
     *  mesma pessoa são proibidos na criação/edição — nunca resolvidos em tempo de aplicação.
     *  {@code excludeId} ignora a própria regra no caso de update. Gatilhos dentro da MESMA regra
     *  não são comparados entre si (duplicata exata já é descartada por {@code sanitizeTriggers}). */
    private @Nullable BusinessError findAmbiguousConflict(UUID personId, List<String> triggers, @Nullable UUID excludeId) {
        val normalizedCandidates = triggers.stream().map(ImportRuleService::normalize).toList();
        for (val other : findAll(personId)) {
            if (excludeId != null && excludeId.equals(other.id())) continue;
            val error = checkConflictAgainstRule(other, normalizedCandidates);
            if (error != null) return error;
        }
        return null;
    }

    private @Nullable BusinessError checkConflictAgainstRule(ImportRule other, List<String> normalizedCandidates) {
        for (val otherTrigger : other.triggers()) {
            val otherNormalized = normalize(otherTrigger);
            for (val candidate : normalizedCandidates) {
                if (candidate.contains(otherNormalized) || otherNormalized.contains(candidate)) {
                    return new BusinessError.Conflict(
                            "importRule.ambiguousTrigger", other.name(), otherTrigger);
                }
            }
        }
        return null;
    }
}
