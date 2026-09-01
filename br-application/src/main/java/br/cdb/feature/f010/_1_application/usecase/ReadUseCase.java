package br.cdb.feature.f010._1_application.usecase;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.cache.ImportRuleCache;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Toda a leitura de regra de nomenclatura da fatia {@code f010} — o par de {@link WriteUseCase},
 * mesmo arranjo CQRS de {@code f002}/{@code f003}/{@code f004}/{@code f006}. Context-wired
 * ({@code Context.tryGet(ReadUseCase.class)}, nunca {@code @Inject}); o {@code ImportRuleResource}
 * lê <b>só</b> daqui.
 *
 * <p>Sem guarda de propriedade explícita ({@code UserGuards}): a fatia é escopada por pessoa na
 * própria query ({@code F010_IMPORT_RULE.COD_PERSON} — {@code findAllByPerson}/
 * {@code findByPersonAndId}), e o {@code personId} vem do {@code /api/{uuid}/…} já validado pelo
 * {@code OwnershipFilter}. Mesma convenção de {@code f004} (tags) e {@code f005} (categorias).
 *
 * <p>Lê do cache off-heap; nunca do banco (ver .claude/plan.md, D1).
 */
@NullMarked
public class ReadUseCase {

    private final ImportRuleCache cache = Context.tryGet(ImportRuleCache.class);

    public List<ImportRule> rules(UUID personId) {
        val result = cache.list(personId.toString());
        if (result.isFailure()) {
            return List.of();
        }
        return result.get();
    }

    /** Guarda implícita: {@code NotFound} quando a regra existe mas é de outra pessoa (ou o cache
     *  não tem entrada nenhuma pra ela). */
    public Result<ImportRule, BusinessError> rule(UUID personId, UUID id) {
        return cache.find(personId.toString(), id)
                .mapFailure(error -> (BusinessError) new BusinessError.NotFound("error.rule.not-found", id));
    }
}
