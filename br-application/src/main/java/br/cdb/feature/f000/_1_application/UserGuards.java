package br.cdb.feature.f000._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.AccountOwnership;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.enterprise.context.RequestScoped;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Guardas de propriedade conta/cartão — fronteira contra IDOR, consumida pelo use case de cada
 * fatia. {@code MON_ACCOUNT}/
 * {@code MON_CARD} não têm coluna de usuário; a propriedade mora só no overlay {@code PERSON_ACCOUNT}. Memoiza
 * (preguiçosamente, no máximo 1 {@code findByPerson} + 1 scan de cartões) por requisição — o snapshot é tirado
 * na primeira consulta, então um guard chamado <em>depois</em> de uma mutação na mesma requisição leria estado
 * pré-mutação. Hoje todo guard roda na entrada, antes de qualquer escrita, então é sempre correto.
 */
@NullMarked
@RequestScoped
@RequiredArgsConstructor
public class UserGuards {

    public static final String DEFAULT_LANGUAGE = "pt-BR";
    public static final String DEFAULT_LOCALE = "pt-BR";

    private final AccountOwnership accountOwnership;

    private @Nullable Set<UUID> ownedAccounts;
    private @Nullable Map<UUID, UUID> accountByCard;

    public Result<Void, BusinessError> ownsAccount(UUID accountId) {
        if (!ownedAccounts().contains(accountId)) {
            return Result.failure(new BusinessError.NotFound("Account not found: " + accountId));
        }
        return Result.success();
    }

    public Result<Void, BusinessError> ownsAccounts(UUID ... accountIds) {
        for (val accountId : accountIds) {
            if (ownsAccount(accountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
        }
        return Result.success();
    }

    public Result<Void, BusinessError> ownsCard(UUID cardId) {
        val accountId = accountByCard().get(cardId);
        if (accountId == null || !ownedAccounts().contains(accountId)) {
            return Result.failure(new BusinessError.NotFound("CreditCard not found: " + cardId));
        }
        return Result.success();
    }

    public Result<Void, BusinessError> ownsCard(UUID accountId, UUID cardId) {
        return ownsAccount(accountId).flatMap(ignored -> {
            if (!accountId.equals(accountByCard().get(cardId))) {
                return Result.failure(new BusinessError.NotFound("CreditCard not found: " + cardId));
            }
            return Result.success();
        });
    }

    /**
     * Checa posse de conta e (se presente) cartão <em>independentemente</em> — não exige que o
     * cartão esteja nessa conta específica. Um cartão de outra conta do mesmo usuário passa aqui;
     * o pareamento conta↔cartão já é regra de negócio de {@code TransactionUseCase.validateCard}
     * (400 {@code BusinessRule}), que não deve virar 404 de propriedade.
     */
    public Result<Void, BusinessError> ownsAccountAndCard(UUID accountId, @Nullable UUID cardId) {
        return ownsAccount(accountId).flatMap(ignored -> cardId == null ? Result.success() : ownsCard(cardId));
    }

    private Set<UUID> ownedAccounts() {
        var cached = ownedAccounts;
        if (cached == null) {
            cached = accountOwnership.ownedAccountIds(HTTPRequest.personId());
            ownedAccounts = cached;
        }
        return cached;
    }

    private Map<UUID, UUID> accountByCard() {
        var cached = accountByCard;
        if (cached == null) {
            cached = MonetaryUseCases.ucCreditCard().list().getOrElse(List.of()).stream()
                    .collect(Collectors.toUnmodifiableMap(CreditCard::id, CreditCard::accountId));
            accountByCard = cached;
        }
        return cached;
    }
}
