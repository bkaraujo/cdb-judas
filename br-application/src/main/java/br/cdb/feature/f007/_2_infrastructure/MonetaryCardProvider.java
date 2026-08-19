package br.cdb.feature.f007._2_infrastructure;

import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f003.F003Api;
import br.cdb.feature.f007._1_application.CreditCardProvider;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * {@link CreditCardProvider} de produção: lê os cartões via {@link F002Api} (HTTP real por
 * {@code InternalApi}, D1 de {@code .claude/plan.md}) — nunca o use case de {@code f003} direto.
 * {@code AccountCardResource} só expõe listagem por conta ({@code GET .../accounts/{id}/cards}), sem
 * rota "todos os cartões da pessoa"; em vez de N chamadas (uma por conta), lê os cartões já embutidos
 * na resposta de {@link F002Api#accounts()} ({@code AccountResponse.cards}, a mesma projeção
 * somente-leitura que {@code f002} mantém para não depender de {@code f003}) — uma única chamada
 * HTTP. O nome da conta a que cada cartão pertence é resolvido na borda (Resource), ao montar a
 * resposta — não faz parte da identidade do cartão.
 */
@NullMarked
public class MonetaryCardProvider implements CreditCardProvider {

    @Override
    public List<F003Api.CardView> creditCards() {
        return  Context.get(F002Api.class).accounts().stream()
                .flatMap(account -> account.cards().stream())
                .map(card -> new F003Api.CardView(card.id(), card.last4(), card.accountId(), card.active()))
                .toList();
    }
}
