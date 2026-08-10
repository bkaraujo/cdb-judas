package br.cdb.feature.f006;

import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f006._2_infrastructure.web.TransactionResource;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cliente tipado da própria API pública de {@code f006}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, em vez de cada consumidor remontar path + DTO de deserialização por conta
 * própria (era o que {@code f009.ReadUseCase} e {@code f005.WriteUseCase} faziam). Mesmo papel de
 * {@code f002.F002Api}/{@code f005.F005Api}.
 *
 * <p>Continua sendo HTTP real via {@link InternalApi} — mesmas rotas públicas
 * ({@link TransactionResource#listAll}, {@link TransactionResource#byCategory}), mesmo
 * {@code AuthenticationFilter}/{@code OwnershipFilter}, token efêmero. O consumidor não alcança
 * serviço, repositório nem o modelo {@code Transaction} de {@code f006}: vê só {@link TransactionView},
 * a projeção mínima que este cliente expõe.
 *
 * <p>Context-wired ({@code Context.tryGet(F006Api.class)}), sem estado próprio: {@link InternalApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F006Api {

    /**
     * Projeção mínima de uma transação para quem lê de fora da fatia. {@code type} vem lowercase
     * ("income"/"expense", ver {@code JsonStorageConfig.transactionEnumModule}) e {@code amount} vem
     * assinado (negativo para despesa), exatamente como o endpoint responde.
     */
    @NullMarked
    public record TransactionView(LocalDate date, BigDecimal amount, String type) {}

    private static InternalApi internalApi() {
        return Context.get(InternalApi.class);
    }

    /**
     * Transações da pessoa, filtradas no servidor — parâmetro nulo não filtra. Evita trazer o
     * histórico inteiro para descartar a maior parte no consumidor.
     */
    public List<TransactionView> transactions(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return List.of(internalApi().get("/accounts/transactions" + query(status, dateFrom, dateTo), TransactionView[].class));
    }

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds}. */
    public List<UUID> transactionIdsByCategories(Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        val qs = categoryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        return List.of(internalApi().get("/accounts/transactions/by-category?categoryIds=" + qs, UUID[].class));
    }

    private static String query(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        val params = new ArrayList<String>();
        if (status != null) params.add("status=" + status);
        if (dateFrom != null) params.add("dateFrom=" + dateFrom);
        if (dateTo != null) params.add("dateTo=" + dateTo);
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }
}
