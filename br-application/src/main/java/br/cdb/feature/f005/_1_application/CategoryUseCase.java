package br.cdb.feature.f005._1_application;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f005._0_domain.UserCategory;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.MessageBus;
import br.commons.Registry;
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
 * Use case da fatia {@code f005} (categories). {@code deleteCategory(DELETE)} publica
 * {@link AccountStreamEvents.Refresh} (SSE de conta — dispatch é responsabilidade única de {@code f999}).
 * A remoção das linhas {@code PERSON_CATEGORY} da subárvore publica
 * {@link CategoryDeleted} (reagido por {@code CategoryDeletedListener}, aqui mesmo); a limpeza do
 * overlay de transações apagadas em cascata publica {@link TransactionsDeleted} em vez de chamar
 * {@code UserTransactionService}/{@code TransactionTagService} diretamente (best-effort,
 * reagido por f006/f004).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class CategoryUseCase {

    private final br.cdb.feature.f006._1_application.usecase.TransactionUseCase ucTransaction =
            Registry.tryGet(br.cdb.feature.f006._1_application.usecase.TransactionUseCase.class);

    private final UserCategoryService userCategoryService;
    private final InternalApi internalApi;

    public List<UserCategory> categories(UUID personId) {
        return userCategoryService.findAll(personId);
    }

    /** Categoria de sistema de transferência da natureza pedida — endpoint interno consumido via
     *  {@code InternalApi} por {@code f006.TransactionUseCase} (nunca chamado pelo frontend). Global
     *  (ver {@link UserCategoryService#findOrCreateTransferCategory}) — {@code personId} não influencia
     *  o resultado, só chega aqui pela simetria de rota {@code /api/{uuid}/categories/transfer}. */
    public UserCategory transferCategory(UUID personId, Transaction.Type nature) {
        return userCategoryService.findOrCreateTransferCategory(nature);
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
                val count = transactionIdsByCategories(personId, subtree).size();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
                MessageBus.submit(new CategoryDeleted(subtree, personId));
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
        return userCategoryService.validateMoveTarget(id, targetId, personId).flatMap(subtree -> {
            MessageBus.submit(new CategoryReassigned(subtree, targetId, personId));
            MessageBus.submit(new CategoryDeleted(subtree, personId));
            return Result.success();
        });
    }

    /** Apaga as transações vinculadas à subárvore inteira e depois a subárvore. */
    private Result<Void, BusinessError> deleteCategoryWithTransactions(List<UUID> subtreeIds, UUID personId) {
        val txIds = transactionIdsByCategories(personId, subtreeIds);
        return deleteLinkedTransactions(txIds, () -> MessageBus.submit(new CategoryDeleted(subtreeIds, personId)));
    }

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds} —
     *  leitura cross-slice síncrona via {@code InternalApi} (endpoint público de f006). */
    private List<UUID> transactionIdsByCategories(UUID personId, List<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        val qs = categoryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        return List.of(internalApi.get("/accounts/transactions/by-category?categoryIds=" + qs, UUID[].class));
    }

    // CPD-OFF
    /** Apaga as transações (via facade), publica {@link TransactionsDeleted} (overlay/vínculo de tag
     *  reagem best-effort), executa {@code afterCleanup} e por fim publica o SSE de conta para cada
     *  conta afetada. Duplicado em {@code TagUseCase} (f004): mesma forma, dono diferente, sem base
     *  comum legítima entre fatias. */
    private Result<Void, BusinessError> deleteLinkedTransactions(List<UUID> txIds, Runnable afterCleanup) {
        val affectedAccountIds = accountIdsOfTransactions(txIds);

        if (ucTransaction.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        MessageBus.submit(new TransactionsDeleted(txIds));

        afterCleanup.run();
        affectedAccountIds.forEach(id -> MessageBus.submit(new AccountStreamEvents.Refresh(id, HTTPRequest.personId())));
        return Result.success();
    }

    private Set<UUID> accountIdsOfTransactions(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }
    // CPD-ON
}
