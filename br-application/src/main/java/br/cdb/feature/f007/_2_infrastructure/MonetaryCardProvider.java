package br.cdb.feature.f007._2_infrastructure;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f007._0_domain.CreditCardProvider;
import br.commons.Registry;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

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

    private final CreditCardUseCase ucCreditCard = Registry.tryGet(CreditCardUseCase.class);

    @Override
    public List<CreditCard> creditCards() {
        return ucCreditCard.list().getOrElse(List.of());
    }
}
