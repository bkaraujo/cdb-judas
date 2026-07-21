package br.cdb.feature.f008._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f008._0_domain.UserTag;
import br.cdb.feature.finance.accounts.core.AccountStreamPublisher;
import br.cdb.feature.finance.accounts.transactions.UserTransactionService;
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
 * Use case da fatia {@code f008} (tags). {@code deleteTag(DELETE)} ainda depende diretamente de
 * {@link UserTransactionService}/{@link AccountStreamPublisher} — overlay e SSE de transações/conta,
 * hoje donos de f005/f002, que ainda não existem como fatias próprias. Transitório: quando f005/f002
 * migrarem, essa limpeza vira reação a {@code TransactionsDeleted} (best-effort) e o import direto
 * desaparece (.claude/refactor.md, catálogo de eventos).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagUseCase {

    private final TransactionUseCase ucTransaction = MonetaryUseCases.ucTransaction();

    private final UserTagService userTagService;
    private final UserTransactionTagService tagLinkService;
    private final UserTransactionService userTransactionService;
    private final AccountStreamPublisher accountStreamPublisher;

    public List<UserTag> tags(UUID personId) {
        return userTagService.findAll(personId);
    }

    public UserTag createTag(UUID personId, String name, String color) {
        return userTagService.create(personId, name, color);
    }

    public Result<UserTag, BusinessError> updateTag(UUID id, String name, String color) {
        return userTagService.update(id, name, color);
    }

    public Result<DeletionOutcome, BusinessError> deleteTag(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        if (strategy == null) {
            val count = userTagService.linkedTransactionIds(personId, id).size();
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            return userTagService.deleteById(id).map(ignored -> new DeletionOutcome.Completed());
        }

        val result = switch (strategy) {
            case MOVE -> userTagService.deleteMoving(id, Objects.requireNonNull(targetId), personId);
            case DELETE -> deleteTagWithTransactions(id, personId);
            case DETACH -> userTagService.deleteDetached(id, personId);
        };
        return result.map(ignored -> new DeletionOutcome.Completed());
    }

    /** Apaga as transações vinculadas à tag e depois a tag em si. */
    private Result<Void, BusinessError> deleteTagWithTransactions(UUID id, UUID personId) {
        return userTagService.findById(id).flatMap(existing -> {
            val txIds = userTagService.linkedTransactionIds(personId, id);
            return deleteLinkedTransactions(txIds, () -> userTagService.deleteById(id));
        });
    }

    /** Apaga as transações (via facade) + vínculo de tag, executa {@code afterCleanup} (apagar a tag
     *  agora desvinculada) e por fim publica o SSE de conta para cada conta afetada — resolvidas antes
     *  do delete, quando as transações ainda existem. Duplicado em {@code CategoryUseCase} (f007):
     *  mesma forma, dono diferente, sem base comum legítima entre fatias. */
    private Result<Void, BusinessError> deleteLinkedTransactions(List<UUID> txIds, Runnable afterCleanup) {
        val affectedAccountIds = accountIdsOfTransactions(txIds);

        if (ucTransaction.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        txIds.forEach(txId -> {
            userTransactionService.deleteByTransaction(txId);
            tagLinkService.deleteByTransaction(txId);
        });

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
