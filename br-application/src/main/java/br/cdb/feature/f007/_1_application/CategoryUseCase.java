package br.cdb.feature.f007._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f005._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f005._1_application.UserTransactionService;
import br.cdb.feature.f007._0_domain.UserCategory;
import br.cdb.feature.f002._1_application.AccountStreamPublisher;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case da fatia {@code f007} (categories). {@code deleteCategory(DELETE)} ainda chama
 * {@link AccountStreamPublisher} direto (SSE de conta, hoje dono de f002 — transitório até essa
 * fatia migrar); a limpeza do overlay de transações apagadas em cascata publica
 * {@link TransactionsDeleted} em vez de chamar {@code UserTransactionService}/
 * {@code UserTransactionTagService} diretamente (best-effort, reagido por f005/f008).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class CategoryUseCase {

    private final br.cdb.context.monetary._1_application.usecase.TransactionUseCase ucTransaction =
            MonetaryUseCases.ucTransaction();

    private final UserCategoryService userCategoryService;
    private final UserTransactionService userTransactionService;
    private final AccountStreamPublisher accountStreamPublisher;

    public List<UserCategory> categories(UUID personId) {
        return userCategoryService.findAll(personId);
    }

    public Result<UserCategory, BusinessError> createCategory(UUID personId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        if (parentId != null
                && userCategoryService.validateParent(parentId, nature) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return userCategoryService.validateUniqueName(personId, nature.name(), name, parentId, null)
                .map(ignored -> userCategoryService.create(personId, name, nature, parentId));
    }

    public Result<UserCategory, BusinessError> updateCategory(UUID personId, UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        return userCategoryService.findById(id).flatMap(existing -> {
            if (existing.isSystem()) {
                return Result.failure(new BusinessError.BusinessRule("Categoria de sistema não pode ser modificada"));
            }
            return userCategoryService.validateUniqueName(personId, existing.nature().name(), name, parentId, id)
                    .map(ignored -> userCategoryService.update(id, name, parentId, active));
        });
    }

    public Result<DeletionOutcome, BusinessError> deleteCategory(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return userCategoryService.subtreeIds(id, personId).flatMap(subtree -> {
            if (strategy == null) {
                val count = userTransactionService.findTransactionIdsByCategories(personId, subtree).size();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
                userCategoryService.deletePlain(subtree, personId);
                return Result.success(new DeletionOutcome.Completed());
            }

            val result = strategy == DeletionStrategy.MOVE
                    ? moveCategorySubtree(id, Objects.requireNonNull(targetId), personId)
                    : deleteCategoryWithTransactions(subtree, personId);
            return result.map(ignored -> new DeletionOutcome.Completed());
        });
    }

    /** Reatribui toda a subárvore para {@code targetId} (já validado pelo service) e apaga a subárvore. */
    private Result<Void, BusinessError> moveCategorySubtree(UUID id, UUID targetId, UUID personId) {
        return userCategoryService.validateMoveTarget(id, targetId, personId).map(subtree -> {
            subtree.forEach(nodeId -> userTransactionService.reassignCategory(nodeId, targetId, personId));
            userCategoryService.deletePlain(subtree, personId);
            return null;
        });
    }

    /** Apaga as transações vinculadas à subárvore inteira e depois a subárvore. */
    private Result<Void, BusinessError> deleteCategoryWithTransactions(List<UUID> subtreeIds, UUID personId) {
        val txIds = userTransactionService.findTransactionIdsByCategories(personId, subtreeIds);
        return deleteLinkedTransactions(txIds, () -> userCategoryService.deletePlain(subtreeIds, personId));
    }

    /** Apaga as transações (via facade), publica {@link TransactionsDeleted} (overlay/vínculo de tag
     *  reagem best-effort), executa {@code afterCleanup} e por fim publica o SSE de conta para cada
     *  conta afetada. Duplicado em {@code TagUseCase} (f008): mesma forma, dono diferente, sem base
     *  comum legítima entre fatias. */
    private Result<Void, BusinessError> deleteLinkedTransactions(List<UUID> txIds, Runnable afterCleanup) {
        val affectedAccountIds = accountIdsOfTransactions(txIds);

        if (ucTransaction.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        MessageBus.submit(new TransactionsDeleted(txIds));

        afterCleanup.run();
        affectedAccountIds.forEach(accountStreamPublisher::upsert);
        return Result.success();
    }

    private Set<UUID> accountIdsOfTransactions(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }
}
