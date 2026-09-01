package br.cdb.feature.f005._1_application.usecase;

import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._1_application.cache.CategoryCache;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Toda a leitura de categoria da fatia {@code f005} — o par de {@link WriteUseCase}, mesmo arranjo
 * CQRS de {@code f002}/{@code f003}/{@code f004}/{@code f006}. Context-wired
 * ({@code Context.tryGet(ReadUseCase.class)}, nunca {@code @Inject}); o {@code CategoryResource} lê
 * <b>só</b> daqui.
 *
 * <p>Categoria não usa {@code UserGuards}: o escopo por pessoa está na própria query
 * ({@code F005_CATEGORY.COD_PERSON}), com o {@code personId} vindo do {@code /api/{uuid}/…} já
 * validado pelo {@code OwnershipFilter}. A exceção é a categoria de sistema "Transferência", que é
 * global e entra em toda listagem (ver {@link UserCategoryService#findAll}).
 *
 * <p>Nota: o nome simples coincide com o {@code ReadUseCase} de {@code f002}/{@code f003}/{@code f004}
 * — quem precisa de mais de um referencia os demais pelo nome completo.
 *
 * <p>Lê do cache off-heap; nunca do banco (ver .claude/plan.md, D1).
 */
@NullMarked
public class ReadUseCase {

    private final CategoryCache cache = Context.tryGet(CategoryCache.class);
    private final UserCategoryService service = Context.tryGet(UserCategoryService.class);

    /** Categorias da pessoa + as duas globais de transferência. */
    public List<Category> categories(UUID personId) {
        val result = cache.list(personId.toString());
        if (result.isFailure()) {
            return List.of();
        }
        return result.get();
    }

    /**
     * Categoria de sistema de transferência da natureza pedida — endpoint interno consumido via
     * {@code InternalApi} por {@code f006.ReadUseCases} (nunca chamado pelo frontend). Global
     * (ver {@link UserCategoryService#findOrCreateTransferCategory}): {@code personId} não influencia
     * o resultado, só chega aqui pela simetria de rota {@code /api/{uuid}/categories/transfer}.
     *
     * <p>Leitura com autocura: semeia a categoria global no primeiro uso em banco fresh/teste, onde
     * a migração que a cria não roda.
     */
    public Category transferCategory(UUID personId, Nature nature) {
        return service.findOrCreateTransferCategory(nature);
    }

    public Result<Category, BusinessError> category(UUID personId, UUID id) {
        val result = cache.find(personId.toString(), id);
        return result.isSuccess() ? Result.success(result.get()) : service.findById(id);
    }

    /** Ids da categoria + toda a subárvore; falha se a categoria não é da pessoa ou é de sistema. */
    public Result<List<UUID>, BusinessError> subtreeIds(UUID id, UUID personId) {
        return service.subtreeIds(id, personId);
    }
}
