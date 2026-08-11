package br.cdb.feature.f000._1_application;

import br.cdb.core.web.error.ProblemDetail;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Parsing de {@code ?strategy=}/{@code targetId} e construção do 409 rico, compartilhados pelos
 *  4 recursos que implementam o contrato uniforme de exclusão (accounts, cards, categories, tags). */
@NullMarked
public final class Deletions {

    private Deletions() {}

    /**
     * Fluxo HTTP completo do contrato uniforme de exclusão: valida {@code ?strategy=}/{@code targetId},
     * delega ao {@code *UseCase} da fatia chamadora via {@code action} e traduz o
     * {@link DeletionOutcome} — 409 rico
     * quando bloqueada por vínculos ({@code "Existem N transações vinculadas " + linkedSuffix}), 204
     * quando concluída. Falhas de negócio viram {@link BusinessException} (borda traduz para HTTP).
     */
    public static Response execute(
            @Nullable String strategy,
            @Nullable UUID targetId,
            Set<DeletionStrategy> allowed,
            Function<@Nullable DeletionStrategy, Result<DeletionOutcome, BusinessError>> action,
            String linkedSuffix
    ) {
        val parsed = parse(strategy, targetId, allowed);
        if (parsed instanceof Result.Failure<DeletionStrategy, BusinessError>(var error)) {
            throw new BusinessException(error);
        }

        return switch (action.apply(parsed.get())) {
            case Result.Failure(var error) -> throw new BusinessException(error);
            case Result.Success(var outcome) -> switch (outcome) {
                case DeletionOutcome.Linked(var count) ->
                        linkedConflict(null, count, "Existem " + count + " transações vinculadas " + linkedSuffix);
                case DeletionOutcome.Completed ignored -> Response.noContent().build();
            };
        };
    }

    /** {@code null} = nenhuma estratégia informada (exclusão simples). Inválida, não permitida
     *  para o recurso, ou {@code MOVE} sem {@code targetId} → {@link BusinessError.Validation} (422). */
    public static Result<@Nullable DeletionStrategy, BusinessError> parse(
            @Nullable String raw,
            @Nullable UUID targetId,
            Set<DeletionStrategy> allowed
    ) {
        if (raw == null || raw.isBlank()) return Result.success(null);

        final DeletionStrategy strategy;
        try {
            strategy = DeletionStrategy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Result.failure(new BusinessError.Validation("Estratégia de exclusão inválida: %s", raw));
        }

        if (!allowed.contains(strategy)) {
            return Result.failure(new BusinessError.Validation("Estratégia de exclusão não suportada para este recurso: %s", strategy));
        }
        if (strategy == DeletionStrategy.MOVE && targetId == null) {
            return Result.failure(new BusinessError.Validation("Estratégia MOVE requer targetId"));
        }
        return Result.success(strategy);
    }

    /** Só para account/card (entidades do contexto monetário). {@code DETACH} nunca chega aqui —
     *  é tratado inteiramente na feature de tags, que não usa {@link TransactionPolicy}. */
    public static TransactionPolicy toPolicy(
            @Nullable DeletionStrategy strategy,
            @Nullable UUID targetId
    ) {
        if (strategy == null) return new TransactionPolicy.Block();
        return switch (strategy) {
            case MOVE -> new TransactionPolicy.Move(Objects.requireNonNull(targetId, "targetId required for MOVE"));
            case DELETE -> new TransactionPolicy.Purge();
            case DETACH -> throw new IllegalArgumentException("DETACH is not a TransactionPolicy strategy");
        };
    }

    public static Response linkedConflict(@Nullable String instance, int count, String detail) {
        val pd = ProblemDetail.of(Response.Status.CONFLICT, instance, detail)
                .withCode("LINKED_TRANSACTIONS")
                .withCount(count);
        return Response.status(pd.status()).entity(pd).build();
    }
}
