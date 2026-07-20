package br.cdb.feature.finance.categories;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.stream.SSE;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserCategoryService {

    private static final String TYPE = "CATEGORY";

    private final UserCategoryRepository repo;
    private final SSE sse;

    public List<UserCategory> findAll(UUID personId) {
        return repo.findAllByPerson(personId);
    }

    public Result<UserCategory, BusinessError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserCategory, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Category not found: " + id)));
    }

    public Result<Void, BusinessError> validateNotMacroCategory(UUID categoryId) {
        return findById(categoryId).flatMap(cat -> {
            if (cat.parentId() == null) {
                return Result.failure(new BusinessError.BusinessRule(
                        "Não é permitido lançar transações em macro-categorias. Selecione uma subcategoria."));
            }
            return Result.success();
        });
    }

    public Result<Void, BusinessError> validateParent(UUID parentId, Transaction.Type nature) {
        return repo.findById(parentId)
                .<Result<Void, BusinessError>>map(parent -> {
                    if (parent.parentId() != null) {
                        return Result.failure(new BusinessError.BusinessRule("Subcategoria não pode ter outra subcategoria como pai"));
                    }
                    if (parent.nature() != nature) {
                        return Result.failure(new BusinessError.BusinessRule("Natureza da subcategoria deve ser igual à do pai"));
                    }
                    return Result.success();
                })
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Category not found: " + parentId)));
    }

    public Result<Void, BusinessError> validateUniqueName(UUID personId, String nature, String name, @Nullable UUID parentId, @Nullable UUID excludeId) {
        val optional = repo.findByNature(personId, Transaction.Type.valueOf(nature)).stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .findFirst();

        if (optional.isPresent()) {
            return Result.failure(new BusinessError.BusinessRule("Já existe uma categoria com o nome '" + name + "' neste nível"));
        }
        return Result.success();
    }

    public UserCategory create(UUID personId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        val saved = repo.save(new UserCategory(UUID.randomUUID(), personId, nature, name, parentId));
        upsert(saved);
        return saved;
    }

    public UserCategory update(UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        val existing = repo.findById(id).orElseThrow(() -> new IllegalStateException("Category not found: " + id));
        val saved = repo.save(new UserCategory(id, existing.personId(), existing.nature(), name, parentId,
                existing.isSystem(), active != null ? active : existing.active(), existing.createdAt(), existing.updatedAt()));
        upsert(saved);
        return saved;
    }

    public UserCategory findOrCreateUncategorizedCategory(UUID personId) {
        return repo.findAllByPerson(personId).stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()) && c.nature() == Transaction.Type.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> {
                    val created = repo.save(new UserCategory(UUID.randomUUID(), personId, Transaction.Type.EXPENSE, "Sem categoria", null, true));
                    upsert(created);
                    return created;
                });
    }

    /** Ids da categoria + toda a subárvore (pré-ordem). Falha se {@code id} não existe ou é de sistema. */
    public Result<List<UUID>, BusinessError> subtreeIds(UUID id, UUID personId) {
        val all = repo.findAllByPerson(personId);
        val root = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (root.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + id));
        if (root.get().isSystem()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de sistema não pode ser excluída"));
        }

        val ids = new ArrayList<UUID>();
        collectSubtree(id, all, ids);
        return Result.success(ids);
    }

    /** Valida o alvo do MOVE (existe, subcategoria, mesma natureza, ativa, fora da subárvore) e, se ok, devolve a subárvore.
     *  Não reatribui transações nem apaga linhas — isso é orquestrado pelo {@code UserUseCase}, que precisa
     *  da subárvore validada antes de tocar em {@code UserTransactionService} (fatia vizinha). */
    public Result<List<UUID>, BusinessError> validateMoveTarget(UUID id, UUID targetId, UUID personId) {
        val all = repo.findAllByPerson(personId);
        val rootOpt = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (rootOpt.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + id));
        val root = rootOpt.get();

        val targetOpt = all.stream().filter(c -> c.id().equals(targetId)).findFirst();
        if (targetOpt.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + targetId));
        val target = targetOpt.get();

        if (target.parentId() == null) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino deve ser uma subcategoria"));
        }
        if (target.nature() != root.nature()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino deve ter a mesma natureza"));
        }
        if (!target.active()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino está inativa"));
        }

        val subtree = new ArrayList<UUID>();
        collectSubtree(id, all, subtree);
        if (subtree.contains(targetId)) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino não pode pertencer à subárvore excluída"));
        }
        return Result.success(subtree);
    }

    /** Subárvore sem nenhum vínculo (ou já desvinculada por quem chama): apaga direto. */
    public void deletePlain(List<UUID> subtreeIds, UUID personId) {
        deleteSubtreeRows(subtreeIds, personId);
    }

    private void collectSubtree(UUID id, List<UserCategory> all, List<UUID> acc) {
        acc.add(id);
        all.stream().filter(c -> id.equals(c.parentId())).forEach(c -> collectSubtree(c.id(), all, acc));
    }

    private void deleteSubtreeRows(List<UUID> ids, UUID personId) {
        for (val nodeId : ids) {
            repo.deleteById(nodeId);
            delete(personId, nodeId);
        }
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(UserCategory category) {
        try {
            sse.dispatch(category.personId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", CategoryResponse.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID personId, UUID categoryId) {
        try {
            sse.dispatch(personId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", categoryId.toString()));
        } catch (Exception ignored) {}
    }
}
