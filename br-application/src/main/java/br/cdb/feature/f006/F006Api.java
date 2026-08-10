package br.cdb.feature.f006;

import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._2_infrastructure.web.TransactionResource;
import br.cdb.feature.f006._2_infrastructure.web.TransferResource;
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
 * <p>Continua sendo HTTP real via {@link HTTPApi} — mesmas rotas públicas de
 * {@link TransactionResource}/{@link TransferResource}, mesmo {@code AuthenticationFilter}/
 * {@code OwnershipFilter}, token efêmero. O consumidor não alcança serviço, repositório nem o modelo
 * {@code Transaction} de {@code f006}: vê só {@link TransactionView}/{@link TransactionDto}, as
 * projeções mínimas que este cliente expõe.
 *
 * <p>Espelha toda a interface pública da fatia (D2 de {@code .claude/plan.md}); {@link #transactions}
 * e {@link #transactionIdsByCategories} são os métodos com consumidor cross-slice hoje ({@code f007}
 * na importação, {@code f009} no dashboard, {@code f005} na exclusão de categoria).
 *
 * <p>Context-wired ({@code Context.tryGet(F006Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F006Api {

    /**
     * Projeção mínima de uma transação para quem lê de fora da fatia. {@code amount} vem assinado
     * (negativo para despesa), {@code status}/{@code type} desserializam como enum (ver
     * {@code JsonStorageConfig.transactionEnumModule}), exatamente como o endpoint responde.
     */
    @NullMarked
    public record TransactionView(
            UUID id,
            UUID accountId,
            String description,
            BigDecimal amount,
            LocalDate date,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable UUID groupId
    ) {}

    /** Espelha {@code TransactionResponse} — devolvida por criação/atualização/patch de status. */
    @NullMarked
    public record TransactionDto(
            UUID id,
            String description,
            BigDecimal amount,
            LocalDate date,
            @Nullable UUID categoryId,
            UUID accountId,
            Transaction.Status status,
            Transaction.Type type,
            UUID costCenterId,
            @Nullable LocalDate paymentDate,
            @Nullable UUID groupId,
            @Nullable Integer installmentNumber,
            @Nullable Integer totalInstallments,
            @Nullable String notes,
            @Nullable UUID cardId,
            List<UUID> tagIds
    ) {}

    /** Corpo de {@link #createTransaction}/{@link #updateTransaction} — espelha {@code TransactionRequest}. */
    @NullMarked
    public record TransactionBody(
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID categoryId,
            @Nullable UUID accountId,
            UUID costCenterId,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable Integer installments,
            @Nullable String editMode,
            @Nullable String deleteMode,
            @Nullable String notes,
            @Nullable UUID cardId,
            @Nullable List<UUID> tagIds
    ) {}

    @NullMarked
    private record PatchStatusBody(Transaction.Status status, LocalDate paymentDate) {}

    @NullMarked
    private record TransferBody(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {}

    private static HTTPApi internalApi() {
        return Context.get(HTTPApi.class);
    }

    // ── Leitura ────────────────────────────────────────────────────

    /** Todo o histórico da pessoa, sem filtro. */
    public List<TransactionView> transactions() {
        return transactions(null, null, null);
    }

    /**
     * Transações da pessoa, filtradas no servidor — parâmetro nulo não filtra. Evita trazer o
     * histórico inteiro para descartar a maior parte no consumidor.
     */
    public List<TransactionView> transactions(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return List.of(internalApi().get("/accounts/transactions" + query(status, dateFrom, dateTo), TransactionView[].class));
    }

    /** Transações de uma conta específica, mesmos filtros de {@link #transactions(String, LocalDate, LocalDate)}. */
    public List<TransactionView> transactionsByAccount(UUID accountId, @Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return List.of(internalApi().get("/accounts/" + accountId + "/transactions" + query(status, dateFrom, dateTo), TransactionView[].class));
    }

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds}. */
    public List<UUID> transactionIdsByCategories(Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        val qs = categoryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        return List.of(internalApi().get("/accounts/transactions/by-category?categoryIds=" + qs, UUID[].class));
    }

    // ── Escrita ────────────────────────────────────────────────────

    public TransactionDto createTransaction(UUID accountId, TransactionBody body) {
        return internalApi().post("/accounts/" + accountId + "/transactions", body, TransactionDto.class);
    }

    public TransactionDto updateTransaction(UUID accountId, UUID transactionId, TransactionBody body) {
        return internalApi().patch("/accounts/" + accountId + "/transactions/" + transactionId, body, TransactionDto.class);
    }

    public TransactionDto patchStatus(UUID accountId, UUID transactionId, Transaction.Status status, LocalDate paymentDate) {
        return internalApi().patch("/accounts/" + accountId + "/transactions/" + transactionId + "/status",
                new PatchStatusBody(status, paymentDate), TransactionDto.class);
    }

    public void deleteTransaction(UUID accountId, UUID transactionId, @Nullable String mode) {
        val qs = mode != null ? "?mode=" + mode : "";
        internalApi().delete("/accounts/" + accountId + "/transactions/" + transactionId + qs);
    }

    public TransactionDto transfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        return internalApi().post("/accounts/transactions/transfer",
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
