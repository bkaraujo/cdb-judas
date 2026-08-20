package br.cdb.feature.f010._1_application.usecase;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.service.ImportRuleService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Toda a mutação de regra de nomenclatura da fatia {@code f010} — o par de {@link ReadUseCase},
 * mesmo arranjo CQRS de {@code f002}/{@code f003}/{@code f004}/{@code f006}. Context-wired
 * ({@code Context.tryGet(WriteUseCase.class)}, nunca {@code @Inject}); o {@code ImportRuleResource}
 * escreve <b>só</b> por aqui.
 *
 * <p>Diferente de {@code f004} (tags): a exclusão aqui não publica evento — nada no resto do
 * sistema referencia uma regra (o casamento com descrição é 100% api-side, ver {@code web/
 * CLAUDE.md}), então não há vínculo cross-slice pra limpar. A guarda de propriedade na exclusão é a
 * mesma de {@code f004.deleteTag}: passa por {@link ReadUseCase#rule} antes de apagar, pra apagar
 * uma regra de outra pessoa vire {@code NotFound} em vez de silenciosamente funcionar (IDOR).
 */
@NullMarked
public class WriteUseCase {

    private final ImportRuleService service = Context.tryGet(ImportRuleService.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    public Result<ImportRule, BusinessError> createRule(
            UUID personId, String name, List<String> triggers, @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        return service.create(personId, name, triggers, accountId, categoryId, costCenterId);
    }

    public Result<ImportRule, BusinessError> updateRule(
            UUID personId, UUID id, String name, List<String> triggers, @Nullable UUID accountId, @Nullable UUID categoryId, @Nullable UUID costCenterId
    ) {
        return service.update(personId, id, name, triggers, accountId, categoryId, costCenterId);
    }

    public Result<Void, BusinessError> deleteRule(UUID personId, UUID id) {
        return reads.rule(personId, id).flatMap(rule -> service.deleteById(rule.id()));
    }
}
