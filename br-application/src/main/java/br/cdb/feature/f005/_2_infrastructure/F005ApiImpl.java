package br.cdb.feature.f005._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._2_infrastructure.web.CategoryResource;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
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
    public UUID transferCategoryId(Nature nature) {
        return get("/categories/transfer?nature=" + nature.name(), TransferCategoryDto.class).id();
    }

    /** Corpo mínimo do endpoint {@code GET /categories/{id}/nature} — espelha
     *  {@code CategoryNatureResponse} sem obrigar o consumidor a conhecê-lo. */
    @NullMarked
    private record CategoryNatureDto(Nature nature) {}

    /** Chave do cache por requisição — ver {@link HTTPRequest#cache}. */
    private static final String NATURE_CACHE = "f005.natureByCategory";

    /**
     * Cacheado pela requisição corrente: os consumidores chamam isto <b>por transação</b> da lista
     * ({@code f006.RequestMapper#toDto}, {@code f999.AccountStreamListener}), e cada chamada é um
     * loopback HTTP completo — um extrato com 300 transações de 5 categorias fazia 300 requisições
     * onde 5 bastam. Categoria não muda de natureza no meio de uma requisição, e o mapa morre com
     * ela ({@code MDCLoggingFilter} chama {@link HTTPRequest#clear}).
     *
     * <p>Fora de uma requisição (boot, job, listener do {@code MessageBus}) não há escopo para
     * cachear e cada chamada vai à fonte, como antes.
     */
    @Override
    public Nature natureOf(UUID categoryId) {
        final Map<UUID, Nature> cache = HTTPRequest.cache(NATURE_CACHE);
        if (cache == null) return fetchNature(categoryId);

        return cache.computeIfAbsent(categoryId, this::fetchNature);
    }

    private Nature fetchNature(UUID categoryId) {
        return get("/categories/" + categoryId + "/nature", CategoryNatureDto.class).nature();
    }
}
