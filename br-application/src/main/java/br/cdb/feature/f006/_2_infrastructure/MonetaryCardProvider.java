package br.cdb.feature.f006._2_infrastructure;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.usecase.ReadUseCase;
import br.cdb.feature.f006._0_domain.CreditCardProvider;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * {@link CreditCardProvider} de produção: lê os cartões (entidade do contexto monetário) direto do
 * contexto. O nome da conta a que cada cartão pertence é resolvido na borda (Resource), ao montar a
 * resposta — não faz parte da identidade do cartão.
 */
@NullMarked
public class MonetaryCardProvider implements CreditCardProvider {

    private final ReadUseCase cardReads = Context.tryGet(ReadUseCase.class);

    @Override
    public List<CreditCard> creditCards() {
        return cardReads.list(HTTPRequest.personId()).getOrElse(List.of());
    }
}
