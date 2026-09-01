package br.cdb.feature.f006._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._2_infrastructure.web.TransactionResource;
import br.cdb.feature.f006._2_infrastructure.web.TransferResource;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return queryTransactions(null, status, dateFrom, dateTo);
    }

    @Override
    public List<TransactionView> transactionsByAccount(UUID accountId, @Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        return queryTransactions(accountId, status, dateFrom, dateTo);
    }

    @Override
    public List<UUID> transactionIdsByCategories(Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        val reads = Context.tryGet(ReadUseCases.class);
        val personId = UUID.fromString(HTTPRequest.personId());
        return reads.transactionIdsByCategories(personId, categoryIds);
    }

    /** Mesma consulta usada por {@code TransactionResource} — {@link ReadUseCases#transactions(UUID,
     *  ReadUseCases.TransactionFilter)} já resolve categoria/tags por transação. */
    private static List<TransactionView> queryTransactions(
            @Nullable UUID accountId, @Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo) {
        val reads = Context.tryGet(ReadUseCases.class);
        val personId = UUID.fromString(HTTPRequest.personId());
        val filter = new ReadUseCases.TransactionFilter(accountId, null, dateFrom, dateTo, status, null);
        val natureCache = new HashMap<UUID, Nature>();
        return unwrap(reads.transactions(personId, filter),
                transactions -> transactions.stream().map(t -> toView(t, natureCache)).toList());
    }

    /** Espelha {@code RequestMapper.toDto}: sinal do valor e {@code type} vêm da natureza efetiva
     *  (pós-estorno), não da natureza crua da categoria. {@code natureCache} evita um lookup de
     *  categoria por transação — mesmo memo por chamada que {@code ReadUseCases.filtered} já usa. */
    private static TransactionView toView(Transaction t, Map<UUID, Nature> natureCache) {
        val categoryId = t.categoryId();
        val categoryNature = categoryId == null
                ? Nature.EXPENSE
                : natureCache.computeIfAbsent(categoryId, id -> Context.get(F005Api.class).natureOf(id));
        int signal = t.calculateSignal(categoryNature);
        Nature effectiveNature = signal > 0 ? Nature.INCOME : Nature.EXPENSE;
        return new TransactionView(t.id(), t.accountId(), t.description(),
                BigDecimal.valueOf(signal).multiply(t.amount()), t.date(), t.status(), effectiveNature,
                t.groupId(), t.cardId());
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

}
