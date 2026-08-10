package br.cdb.feature.f002;

import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f002._2_infrastructure.web.ClosingResource;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Cliente tipado da própria API pública de {@code f002}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, em vez de cada consumidor remontar path + DTO de deserialização por conta
 * própria (era o que {@code f006.ReadUseCases.closingPeriod} fazia). Mesmo papel de
 * {@code f005.F005Api}.
 *
 * <p>Continua sendo HTTP real via {@link InternalApi} — mesma rota pública
 * ({@link ClosingResource#get}), mesmo {@code AuthenticationFilter}/{@code OwnershipFilter}, token
 * efêmero. O acoplamento do consumidor é só com esta classe; nada de serviço/repositório de
 * {@code f002} vaza.
 *
 * <p>Context-wired ({@code Context.tryGet(F002Api.class)}), sem estado próprio: {@link InternalApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F002Api {

    /** Corpo mínimo do endpoint {@code GET /accounts/closing} — espelha {@code ClosingResponse} sem
     *  obrigar o consumidor a conhecê-lo. */
    @NullMarked
    private record ClosingDto(@Nullable String period) {}

    private static InternalApi internalApi() {
        return Context.get(InternalApi.class);
    }

    /** Período de fechamento contábil vigente ({@code yyyy-MM}) ou {@code null} quando não há. */
    public ClosedPeriod closingPeriod() {
        val result = internalApi().get("/accounts/closing", ClosingDto.class).period();
        return ClosedPeriod.of(result);
    }
}
