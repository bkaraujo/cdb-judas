package br.cdb.feature.f005._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Toda a mutação de categoria da fatia {@code f005} — o par de {@link ReadUseCase}, mesmo arranjo
 * CQRS de {@code f002}/{@code f003}/{@code f004}/{@code f006}. Context-wired
 * ({@code Context.tryGet(WriteUseCase.class)}, nunca {@code @Inject}); o {@code CategoryResource}
 * escreve <b>só</b> por aqui.
 *
 * <p>{@code deleteCategory(DELETE)} publica {@link AccountStreamEvents.Refresh} (SSE de conta —
 * dispatch é responsabilidade única de {@code f999}). A remoção das linhas {@code F005_CATEGORY} da
 * subárvore publica {@link CategoryDeleted} (reagido por {@code CategoryDeletedListener}, na própria
 * fatia); a limpeza do vínculo das transações apagadas em cascata publica
 * {@link TransactionsDeleted} em vez de chamar o vínculo de categoria (f006) ou de tag (f004)
 * diretamente (best-effort, reagido por f006/f004).
 *
 * <p>A leitura cross-slice das transações vinculadas é HTTP real contra o endpoint público de f006,
 * via {@link F006Api} — o cliente tipado que a fatia dona publica sobre {@code f000.InternalApi}.
 *
 * <p>Nota: o nome simples coincide com o {@code WriteUseCase} de {@code f002}/{@code f003}/{@code f004}
 * — quem precisa de mais de um referencia os demais pelo nome completo.
 */
@NullMarked
public class WriteUseCase {

    private final UserCategoryService service = Context.tryGet(UserCategoryService.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final ReadUseCases transactionReads = Context.tryGet(ReadUseCases.class);
    private final WriteUseCases transactionWrites = Context.tryGet(WriteUseCases.class);


    public Result<Category, BusinessError> createCategory(UUID personId, String name, Nature nature, @Nullable UUID parentId) {
        if (parentId != null
                && service.validateParent(parentId, nature) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return service.validateUniqueName(personId, nature.name(), name, parentId, null)
                .map(ignored -> service.create(personId, name, nature, parentId));
    }

    public Result<Category, BusinessError> updateCategory(UUID personId, UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        return reads.category(id).flatMap(existing -> {
            if (existing.isSystem()) {
                return Result.failure(new BusinessError.BusinessRule("category.systemCannotBeModified"));
            }
            return service.validateUniqueName(personId, existing.nature().name(), name, parentId, id)
                    .map(ignored -> service.update(id, name, parentId, active));
        });
    }

    public Result<DeletionOutcome, BusinessError> deleteCategory(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return reads.subtreeIds(id, personId).flatMap(subtree -> {
            if (strategy == null) {
                val count = transactionIdsByCategories(subtree).size();
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
        return service.validateMoveTarget(id, targetId, personId).flatMap(subtree -> {
            MessageBus.submit(new CategoryReassigned(subtree, targetId, personId));
            MessageBus.submit(new CategoryDeleted(subtree, personId));
            return Result.success();
        });
    }

    /** Apaga as transações vinculadas à subárvore inteira e depois a subárvore. */
    private Result<Void, BusinessError> deleteCategoryWithTransactions(List<UUID> subtreeIds, UUID personId) {
        val txIds = transactionIdsByCategories(subtreeIds);
        return deleteLinkedTransactions(txIds, () -> MessageBus.submit(new CategoryDeleted(subtreeIds, personId)));
    }

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds} —
     *  leitura cross-slice síncrona via {@link F006Api} (endpoint público de f006). */
    private List<UUID> transactionIdsByCategories(List<UUID> categoryIds) {
        return Context.get(F006Api.class).transactionIdsByCategories(categoryIds);
    }

    // CPD-OFF
    /** Apaga as transações (engine de f006), publica {@link TransactionsDeleted} (overlay/vínculo de
     *  tag reagem best-effort), executa {@code afterCleanup} e por fim publica o SSE de conta para
     *  cada conta afetada. Duplicado em {@code f004}: mesma forma, dono diferente, sem base comum
     *  legítima entre fatias. */
    private Result<Void, BusinessError> deleteLinkedTransactions(List<UUID> txIds, Runnable afterCleanup) {
        val affectedAccountIds = accountIdsOfTransactions(txIds);

        if (transactionWrites.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        MessageBus.submit(new TransactionsDeleted(txIds));

        afterCleanup.run();
        affectedAccountIds.forEach(id -> MessageBus.submit(new AccountStreamEvents.Refresh(id, HTTPRequest.personId())));
        return Result.success();
    }

    private Set<UUID> accountIdsOfTransactions(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return transactionReads.transactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }
    // CPD-ON
}
