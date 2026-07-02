package br.community.feature.user.accounts.transactions.importer;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Card;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;

/**
 * {@link CreditCardProvider} de produção: lê os cartões do contexto monetário e resolve o nome da
 * conta real a que cada um pertence.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class MonetaryCardProvider implements CreditCardProvider {

    private final MonetaryContext monetaryContext;

    @Override
    public List<CreditCard> creditCards() {
        return monetaryContext.listCards().getOrElse(List.of()).stream()
                .map(this::toCreditCard)
                .filter(Objects::nonNull)
                .toList();
    }

    private @Nullable CreditCard toCreditCard(Card card) {
        return switch (monetaryContext.findAccount(card.accountId())) {
            case Result.Success(var account) -> new CreditCard(card.id(), card.accountId(), account.name(), card.last4());
            case Result.Failure(var ignored) -> null;
        };
    }
}
