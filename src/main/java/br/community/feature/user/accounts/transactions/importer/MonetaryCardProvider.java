package br.community.feature.user.accounts.transactions.importer;

import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.CreditCard;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * {@link CreditCardProvider} de produção: lê os cartões (entidade do contexto monetário) direto do
 * contexto. O nome da conta a que cada cartão pertence é resolvido na borda (Resource), ao montar a
 * resposta — não faz parte da identidade do cartão.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class MonetaryCardProvider implements CreditCardProvider {

    private final MonetaryContext monetaryContext;

    @Override
    public List<CreditCard> creditCards() {
        return monetaryContext.listCards().getOrElse(List.of());
    }
}
