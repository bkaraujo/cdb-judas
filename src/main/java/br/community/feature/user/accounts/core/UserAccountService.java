package br.community.feature.user.accounts.core;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.infra.persistence.features.UserAccountJDBCRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

/** Serviço de acesso ao overlay de conta por utilizador ({@code USER_ACCOUNT}). */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountJDBCRepository repository;
    private final MonetaryContext monetaryContext;

    @Nullable
    public UserAccount find(String userId, UUID accountId) {
        return repository.find(userId, accountId).orElse(null);
    }

    public List<UserAccount> findByUser(String userId) {
        return repository.findByUser(userId);
    }

    /** Cartões do utilizador: overlays cuja conta monetária é do tipo {@code CREDIT_CARD}. */
    public List<UserAccount> findCreditCards(String userId) {
        return findByUser(userId).stream()
                .filter(ua -> isType(ua.accountId(), Account.Type.CREDIT_CARD))
                .toList();
    }

    /** Cartões vinculados a uma conta (ex.: a conta corrente que paga a fatura). */
    public List<UserAccount> findCreditCardsByLinkedAccount(String userId, UUID accountId) {
        return findCreditCards(userId).stream()
                .filter(ua -> accountId.equals(ua.linkedAccountId()))
                .toList();
    }

    public void save(UserAccount ua) {
        repository.save(ua);
    }

    public void delete(String userId, UUID accountId) {
        repository.delete(userId, accountId);
    }

    /** Remove os cartões vinculados a uma conta apagada (cascata antes ficava no contexto monetary). */
    public void deleteCardsLinkedTo(String userId, UUID accountId) {
        findCreditCardsByLinkedAccount(userId, accountId)
                .forEach(card -> delete(userId, card.accountId()));
    }

    /**
     * Política da feature: um cartão de crédito deve estar vinculado a uma conta corrente.
     * Devolve o erro de negócio se a regra for violada, ou vazio se válida (ou não for cartão).
     */
    @Nullable
    public DomainError validateCardLink(String type, @Nullable UUID linkedAccountId) {
        if (!Account.Type.CREDIT_CARD.name().equalsIgnoreCase(type)) return null;
        if (linkedAccountId == null) {
            return new DomainError.BusinessRule("Credit card must be linked to a checking account");
        }
        if (!isType(linkedAccountId, Account.Type.CHECKING)) {
            return new DomainError.BusinessRule("Credit card must be linked to a checking account");
        }
        return null;
    }

    private boolean isType(UUID accountId, Account.Type type) {
        return monetaryContext.findAccount(accountId) instanceof Result.Success<Account, DomainError>(var account)
                && account.type() == type;
    }
}
