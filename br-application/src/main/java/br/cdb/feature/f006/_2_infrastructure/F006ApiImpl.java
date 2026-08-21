package br.cdb.feature.f006._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._2_infrastructure.web.TransactionResource;
import br.cdb.feature.f006._2_infrastructure.web.TransferResource;
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
 * <p>Continua sendo HTTP real via {@link HTTPApi} — mesmas rotas públicas de
 * {@link TransactionResource}/{@link TransferResource}, mesmo {@code AuthenticationFilter}/
 * {@code OwnershipFilter}, token efêmero. O consumidor não alcança serviço, repositório nem o modelo
 * {@code Transaction} de {@code f006}: vê só {@link F006Api.TransactionView}/{@link F006Api.TransactionDto},
 * as projeções mínimas que este cliente expõe.
 *
 * <p>Espelha toda a interface pública da fatia (D2 de {@code .claude/plan.md}); {@link #transactions}
 * e {@link #transactionIdsByCategories} são os métodos com consumidor cross-slice hoje ({@code f007}
 * na importação, {@code f009} no dashboard, {@code f005} na exclusão de categoria).
 *
 * <p>Context-wired ({@code Context.get(F006Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F006ApiImpl extends AbstractApiClient implements F006Api {

    @NullMarked
    private record PatchStatusBody(Status status, LocalDate paymentDate) {}

    @NullMarked
    private record TransferBody(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {}

    // ── Leitura ────────────────────────────────────────────────────

    /** Todo o histórico da pessoa, sem filtro. */
    @Override
    public List<TransactionView> transactions() {
        return transactions(null, null, null);
    }

    @Override
    public List<TransactionView> transactions(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return list("/accounts/transactions" + query(status, dateFrom, dateTo), TransactionView[].class);
    }

    @Override
    public List<TransactionView> transactionsByAccount(UUID accountId, @Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return list("/accounts/" + accountId + "/transactions" + query(status, dateFrom, dateTo), TransactionView[].class);
    }

    @Override
    public List<UUID> transactionIdsByCategories(Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        val qs = categoryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        return list("/accounts/transactions/by-category?categoryIds=" + qs, UUID[].class);
    }

    // ── Escrita ────────────────────────────────────────────────────

    @Override
    public TransactionDto createTransaction(UUID accountId, TransactionBody body) {
        return post("/accounts/" + accountId + "/transactions", body, TransactionDto.class);
    }

    @Override
    public TransactionDto updateTransaction(UUID accountId, UUID transactionId, TransactionBody body) {
        return patch("/accounts/" + accountId + "/transactions/" + transactionId, body, TransactionDto.class);
    }

    @Override
    public TransactionDto patchStatus(UUID accountId, UUID transactionId, Status status, LocalDate paymentDate) {
        return patch("/accounts/" + accountId + "/transactions/" + transactionId + "/status",
                new PatchStatusBody(status, paymentDate), TransactionDto.class);
    }

    @Override
    public void deleteTransaction(UUID accountId, UUID transactionId, @Nullable String mode) {
        val qs = mode != null ? "?mode=" + mode : "";
        delete("/accounts/" + accountId + "/transactions/" + transactionId + qs);
    }

    @Override
    public TransactionDto transfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        return post("/accounts/transactions/transfer",
                new TransferBody(fromAccountId, toAccountId, date, amount), TransactionDto.class);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static String query(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        val params = new ArrayList<String>();
        if (status != null) params.add("status=" + status);
        if (dateFrom != null) params.add("dateFrom=" + dateFrom);
        if (dateTo != null) params.add("dateTo=" + dateTo);
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }
}
