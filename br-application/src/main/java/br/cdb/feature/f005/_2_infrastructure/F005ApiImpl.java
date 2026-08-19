package br.cdb.feature.f005._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f005._2_infrastructure.web.CategoryResource;
import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Cliente tipado da própria API pública de {@code f005}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, em vez de cada consumidor remontar path + DTO de deserialização por conta
 * própria (era o que {@code f006.ReadUseCases.transferCategoryId} fazia).
 *
 * <p>Continua sendo HTTP real via {@link HTTPApi} — mesma rota pública
 * ({@link CategoryResource#transferCategory}), mesmo {@code AuthenticationFilter}/
 * {@code OwnershipFilter}, token efêmero. O acoplamento do consumidor é só com esta classe e com
 * tipos que já são vocabulário compartilhado; nada de serviço/repositório de {@code f005} vaza.
 *
 * <p>Context-wired ({@code Context.get(F005Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F005ApiImpl extends AbstractApiClient implements F005Api {

    /** Corpo mínimo do endpoint {@code GET /categories/transfer} — espelha
     *  {@code TransferCategoryResponse} sem obrigar o consumidor a conhecê-lo. */
    @NullMarked
    private record TransferCategoryDto(UUID id) {}

    @Override
    public UUID transferCategoryId(Transaction.Type nature) {
        return get("/categories/transfer?nature=" + nature.name(), TransferCategoryDto.class).id();
    }
}
