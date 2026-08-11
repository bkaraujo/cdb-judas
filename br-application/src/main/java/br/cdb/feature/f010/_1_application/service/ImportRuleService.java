package br.cdb.feature.f010._1_application.service;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Context-wired como os demais serviços de fatia (o par {@code ReadUseCase}/{@code WriteUseCase} o
 *  resolve com {@code Context.tryGet}); sem CDI. */
@NullMarked
public class ImportRuleService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private final ImportRuleRepository repository = Context.get(ImportRuleRepository.class);

    /** Uppercase + remove acentos, pra casar "Água" com "AGUA" na comparação de padrões. */
    public static String normalize(String s) {
        val decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toUpperCase(Locale.ROOT).trim();
    }

    public List<ImportRule> findAll(UUID personId) {
        return repository.findAllByPerson(personId);
    }

    public Result<ImportRule, BusinessError> find(UUID personId, UUID id) {
        return repository.findByPersonAndId(personId, id)
                .<Result<ImportRule, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Regra não encontrada: %s", id)));
    }

    public Result<ImportRule, BusinessError> create(
            UUID personId, String name, @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        val conflict = findAmbiguousConflict(personId, name, null);
        if (conflict != null) return Result.failure(conflict);

        val saved = repository.save(new ImportRule(UUID.randomUUID(), personId, name, accountId, categoryId, costCenterId, null));
        return Result.success(saved);
    }

    public Result<ImportRule, BusinessError> update(
            UUID personId, UUID id, String name, @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        return find(personId, id).flatMap(existing -> {
            val conflict = findAmbiguousConflict(personId, name, id);
            if (conflict != null) return Result.failure(conflict);

            val saved = repository.save(new ImportRule(id, existing.personId(), name, accountId, categoryId, costCenterId, existing.createdAt()));
            return Result.success(saved);
        });
    }

    /** Sem estratégia e sem vínculos: exclusão simples, nada reage a isso. */
    public Result<Void, BusinessError> deleteById(UUID id) {
        repository.deleteById(id);
        return Result.success();
    }

    /** Regras ambíguas (padrão substring de outra, em qualquer direção) são proibidas na
     *  criação/edição — nunca resolvidas em tempo de aplicação. {@code excludeId} ignora a
     *  própria regra no caso de update. */
    private @Nullable BusinessError findAmbiguousConflict(UUID personId, String name, @Nullable UUID excludeId) {
        val normalized = normalize(name);
        for (val other : repository.findAllByPerson(personId)) {
            if (excludeId != null && excludeId.equals(other.id())) continue;
            val otherNormalized = normalize(other.name());
            if (normalized.contains(otherNormalized) || otherNormalized.contains(normalized)) {
                return new BusinessError.Conflict("Padrão ambíguo com a regra existente '%s'", other.name());
            }
        }
        return null;
    }
}
