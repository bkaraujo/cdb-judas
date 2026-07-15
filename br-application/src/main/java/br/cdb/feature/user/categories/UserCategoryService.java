package br.cdb.feature.user.categories;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.feature.user.accounts.core.AccountStreamPublisher;
import br.cdb.feature.user.accounts.transactions.UserTransactionService;
import br.cdb.feature.user.categories.core.CategoryResponse;
import br.cdb.feature.user.stream.SSE;
import br.cdb.feature.user.tags.UserTransactionTagService;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserCategoryService {

    private static final String TYPE = "CATEGORY";

    private final TransactionUseCase ucTransaction = MonetaryContext.ucTransaction();

    private final UserCategoryRepository repo;
    private final UserTransactionService userTransactionService;
    private final UserTransactionTagService tagLinkService;
    private final AccountStreamPublisher accountStreamPublisher;
    private final SSE sse;

    public List<UserCategory> findAll(UUID userId) {
        return repo.findAllByUser(userId);
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

    public Result<Void, BusinessError> validateUniqueName(UUID userId, String nature, String name, @Nullable UUID parentId, @Nullable UUID excludeId) {
        val optional = repo.findByNature(userId, Transaction.Type.valueOf(nature)).stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .findFirst();

        if (optional.isPresent()) {
            return Result.failure(new BusinessError.BusinessRule("Já existe uma categoria com o nome '" + name + "' neste nível"));
        }
        return Result.success();
    }

    public UserCategory create(UUID userId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        val saved = repo.save(new UserCategory(UUID.randomUUID(), userId, nature, name, parentId));
        upsert(saved);
        return saved;
    }

    public UserCategory update(UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        val existing = repo.findById(id).orElseThrow(() -> new IllegalStateException("Category not found: " + id));
        val saved = repo.save(new UserCategory(id, existing.userId(), existing.nature(), name, parentId,
                existing.isSystem(), active != null ? active : existing.active(), existing.createdAt(), existing.updatedAt()));
        upsert(saved);
        return saved;
    }

    public UserCategory findOrCreateUncategorizedCategory(UUID userId) {
        return repo.findAllByUser(userId).stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()) && c.nature() == Transaction.Type.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> {
                    val created = repo.save(new UserCategory(UUID.randomUUID(), userId, Transaction.Type.EXPENSE, "Sem categoria", null, true));
                    upsert(created);
                    return created;
                });
    }

    /** Ids da categoria + toda a subárvore (pré-ordem). Falha se {@code id} não existe ou é de sistema. */
    public Result<List<UUID>, BusinessError> subtreeIds(UUID id, UUID userId) {
        val all = repo.findAllByUser(userId);
        val root = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (root.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + id));
        if (root.get().isSystem()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de sistema não pode ser excluída"));
        }

        val ids = new ArrayList<UUID>();
        collectSubtree(id, all, ids);
        return Result.success(ids);
    }

    public List<UUID> linkedTransactionIds(List<UUID> subtreeIds, UUID userId) {
        return userTransactionService.findTransactionIdsByCategories(userId, subtreeIds);
    }

    /** Reatribui toda a subárvore para {@code targetId} (subcategoria da mesma natureza, fora dela) e apaga a subárvore. */
    public Result<Void, BusinessError> deleteMoving(UUID id, UUID targetId, UUID userId) {
        val validation = validateMoveTarget(id, targetId, userId);
        if (validation instanceof Result.Failure<List<UUID>, BusinessError>(var error)) return Result.failure(error);
        val subtree = validation.get();

        for (val nodeId : subtree) {
            userTransactionService.reassignCategory(nodeId, targetId, userId);
        }
        deleteSubtreeRows(subtree, userId);
        return Result.success();
    }

    /** Valida o alvo do MOVE (existe, subcategoria, mesma natureza, ativa, fora da subárvore) e, se ok, devolve a subárvore. */
    private Result<List<UUID>, BusinessError> validateMoveTarget(UUID id, UUID targetId, UUID userId) {
        val all = repo.findAllByUser(userId);
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

    /** Apaga as transações vinculadas à subárvore inteira (via facade) e depois a subárvore. */
    public Result<Void, BusinessError> deleteWithTransactions(List<UUID> subtreeIds, UUID userId) {
        val txIds = linkedTransactionIds(subtreeIds, userId);
        val affectedAccountIds = accountIdsOf(txIds);

        if (ucTransaction.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        txIds.forEach(txId -> {
            userTransactionService.deleteByTransaction(txId);
            tagLinkService.deleteByTransaction(txId);
        });

        deleteSubtreeRows(subtreeIds, userId);
        affectedAccountIds.forEach(accountStreamPublisher::upsert);
        return Result.success();
    }

    /** Contas distintas das transações apagadas — resolvidas antes do delete, quando ainda existem. */
    private Set<UUID> accountIdsOf(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }

    /** Subárvore sem nenhum vínculo: apaga direto. */
    public void deletePlain(List<UUID> subtreeIds, UUID userId) {
        deleteSubtreeRows(subtreeIds, userId);
    }

    private void collectSubtree(UUID id, List<UserCategory> all, List<UUID> acc) {
        acc.add(id);
        all.stream().filter(c -> id.equals(c.parentId())).forEach(c -> collectSubtree(c.id(), all, acc));
    }

    private void deleteSubtreeRows(List<UUID> ids, UUID userId) {
        for (val nodeId : ids) {
            repo.deleteById(nodeId);
            delete(userId, nodeId);
        }
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(UserCategory category) {
        try {
            sse.dispatch(category.userId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", CategoryResponse.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID userId, UUID categoryId) {
        try {
            sse.dispatch(userId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", categoryId.toString()));
        } catch (Exception ignored) {}
    }
}
