package br.cdb.feature.f004._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.TagDeleted;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f004._0_domain.UserTag;
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
 * Use case da fatia {@code f004} (tags). {@code deleteTag(DELETE)} publica
 * {@link AccountStreamEvents.Refresh} (SSE de conta — dispatch é responsabilidade única de {@code f999}).
 * MOVE reatribui os vínculos imperativamente ({@code UserTagService.reassignMoving},
 * re-key) e então publica {@link TagDeleted}; as demais estratégias publicam direto — em todo caso
 * quem remove a linha {@code PERSON_TAG} e o vínculo remanescente é {@code TagDeletedListener}
 * (aqui mesmo), best-effort. A limpeza de overlay/vínculo das transações apagadas (DELETE) publica
 * {@link TransactionsDeleted}, reagida por {@code TransactionOverlayListener} (f006) e
 * {@code TagTransactionListener} (aqui mesmo).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagUseCase {

    private final br.cdb.context.monetary._1_application.usecase.TransactionUseCase ucTransaction =
            MonetaryUseCases.ucTransaction();

    private final UserTagService userTagService;

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
            return userTagService.findById(id).map(ignored -> {
                MessageBus.submit(new TagDeleted(id, personId));
                return new DeletionOutcome.Completed();
            });
        }

        val result = switch (strategy) {
            case MOVE -> moveTag(id, Objects.requireNonNull(targetId), personId);
            case DELETE -> deleteTagWithTransactions(id, personId);
            case DETACH -> detachTag(id, personId);
        };
        return result.map(ignored -> new DeletionOutcome.Completed());
    }

    /** Reatribui os vínculos (imperativo) e só então publica {@link TagDeleted} — os vínculos já
     *  movidos tornam a purga do listener um no-op. */
    private Result<Void, BusinessError> moveTag(UUID id, UUID targetId, UUID personId) {
        return userTagService.reassignMoving(id, targetId, personId)
                .ifSuccess(ignored -> MessageBus.submit(new TagDeleted(id, personId)));
    }

    /** Sem re-key a fazer: publicar {@link TagDeleted} já é suficiente (o listener desvincula e apaga a tag). */
    private Result<Void, BusinessError> detachTag(UUID id, UUID personId) {
        return userTagService.findById(id).flatMap(ignored -> {
            MessageBus.submit(new TagDeleted(id, personId));
            return Result.success();
        });
    }

    /** Apaga as transações vinculadas à tag e depois publica {@link TagDeleted}. */
    private Result<Void, BusinessError> deleteTagWithTransactions(UUID id, UUID personId) {
        return userTagService.findById(id).flatMap(existing -> {
            val txIds = userTagService.linkedTransactionIds(personId, id);
            return deleteLinkedTransactions(txIds, () -> MessageBus.submit(new TagDeleted(id, personId)));
        });
    }

    // CPD-OFF
    /** Apaga as transações (via facade), publica {@link TransactionsDeleted} (overlay/vínculo de tag
     *  reagem best-effort), executa {@code afterCleanup} (apagar a tag agora desvinculada) e por fim
     *  publica o SSE de conta para cada conta afetada — resolvidas antes do delete, quando as
     *  transações ainda existem. Duplicado em {@code CategoryUseCase} (f005): mesma forma, dono
     *  diferente, sem base comum legítima entre fatias. */
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
