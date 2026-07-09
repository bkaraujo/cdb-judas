package br.community.feature.user.tags;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.accounts.core.AccountStreamPublisher;
import br.community.feature.user.accounts.transactions.UserTransactionService;
import br.community.feature.user.stream.SSE;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTagService {

    private static final String TYPE = "TAG";

    private final UserTagRepository repo;
    private final UserTransactionTagService tagLinkService;
    private final UserTransactionService userTransactionService;
    private final MonetaryContext monetaryContext;
    private final AccountStreamPublisher accountStreamPublisher;
    private final SSE sse;

    public List<UserTag> findAll(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Result<UserTag, DomainError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserTag, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Tag not found: " + id)));
    }

    public UserTag create(UUID userId, String name, String color) {
        val saved = repo.save(new UserTag(UUID.randomUUID(), userId, name, color, null));
        upsert(saved);
        return saved;
    }

    public Result<UserTag, DomainError> update(UUID id, String name, String color) {
        return findById(id).map(existing -> {
            val saved = repo.save(new UserTag(id, existing.userId(), name, color, existing.createdAt()));
            upsert(saved);
            return saved;
        });
    }

    public List<UUID> linkedTransactionIds(UUID userId, UUID tagId) {
        return tagLinkService.findTransactionIdsByTag(userId, tagId);
    }

    /** Sem estratégia e sem vínculos: exclusão simples. */
    public Result<Void, DomainError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            repo.deleteById(id);
            delete(existing.userId(), id);
            return null;
        });
    }

    public Result<Void, DomainError> deleteMoving(UUID id, UUID targetId, UUID userId) {
        return findById(id).flatMap(ignoredSource -> findById(targetId).flatMap(target -> {
            if (target.id().equals(id)) {
                return Result.<Void>failure(new DomainError.BusinessRule("Tag de destino deve ser diferente da origem"));
            }
            tagLinkService.reassignTag(id, targetId, userId);
            repo.deleteById(id);
            delete(userId, id);
            return Result.success();
        }));
    }

    /** Apaga as transações vinculadas à tag (via facade), depois a tag em si. */
    public Result<Void, DomainError> deleteWithTransactions(UUID id, UUID userId) {
        return findById(id).flatMap(ignored -> {
            val txIds = tagLinkService.findTransactionIdsByTag(userId, id);
            val affectedAccountIds = accountIdsOf(txIds);

            if (monetaryContext.deleteTransactions(txIds) instanceof Result.Failure<Void, DomainError>(var error)) {
                return Result.failure(error);
            }
            txIds.forEach(txId -> {
                userTransactionService.deleteByTransaction(txId);
                tagLinkService.deleteByTransaction(txId);
            });

            repo.deleteById(id);
            delete(userId, id);
            affectedAccountIds.forEach(accountStreamPublisher::upsert);
            return Result.success();
        });
    }

    /** Contas distintas das transações apagadas — resolvidas antes do delete, quando ainda existem. */
    private Set<UUID> accountIdsOf(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return monetaryContext.listTransactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }

    /** Desvincula (apaga só a associação) e exclui a tag; transações permanecem intactas. */
    public Result<Void, DomainError> deleteDetached(UUID id, UUID userId) {
        return findById(id).map(existing -> {
            tagLinkService.deleteByTag(userId, id);
            repo.deleteById(id);
            delete(userId, id);
            return null;
        });
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(UserTag tag) {
        try {
            sse.dispatch(tag.userId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", tag));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID userId, UUID tagId) {
        try {
            sse.dispatch(userId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", tagId.toString()));
        } catch (Exception ignored) {}
    }
}
