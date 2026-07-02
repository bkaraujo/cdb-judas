package br.community.feature.user.accounts.transactions.importer;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.accounts.core.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * {@link CreditCardProvider} de produção: lê os cartões do utilizador corrente no overlay
 * {@code USER_ACCOUNT} (via {@link UserAccountService}) e completa o nome a partir da conta monetária
 * (via {@link MonetaryContext}).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserCreditCardProvider implements CreditCardProvider {

    private final UserAccountService userAccountService;
    private final MonetaryContext monetaryContext;

    @Override
    public List<CreditCard> creditCards() {
        val userId = CurrentUser.getId();
        return userAccountService.findCreditCards(userId).stream()
                .map(ua -> new CreditCard(ua.accountId(), nameOf(ua.accountId()), ua.last4(), ua.linkedAccountId()))
                .toList();
    }

    private String nameOf(java.util.UUID accountId) {
        return monetaryContext.findAccount(accountId) instanceof Result.Success<Account, DomainError>(var account)
                ? account.name() : "";
    }
}
