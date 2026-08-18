package br.cdb.feature.f002;

import br.cdb.core.View;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f002._2_infrastructure.web.AccountBalanceResource;
import br.cdb.feature.f002._2_infrastructure.web.AccountResource;
import br.cdb.feature.f002._2_infrastructure.web.ClosingResource;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Cliente tipado da própria API pública de {@code f002}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, em vez de cada consumidor remontar path + DTO de deserialização por conta
 * própria (era o que {@code f006.ReadUseCases.closingPeriod} fazia). Mesmo papel de
 * {@code f005.F005Api}.
 *
 * <p>Continua sendo HTTP real via {@link HTTPApi} — mesmas rotas públicas
 * ({@link AccountResource}, {@link AccountBalanceResource}, {@link ClosingResource#get}), mesmo
 * {@code AuthenticationFilter}/{@code OwnershipFilter}, token efêmero. O acoplamento do consumidor é
 * só com esta classe; nada de serviço/repositório de {@code f002} vaza.
 *
 * <p>Espelha toda a interface pública da fatia (D2 de {@code .claude/plan.md}), inclusive métodos sem
 * consumidor hoje — mutação de conta e fechamento continuam sem chamador cross-slice.
 *
 * <p>Context-wired ({@code Context.tryGet(F002Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F002Api {

    /** Corpo mínimo do endpoint {@code GET /accounts/closing} — espelha {@code ClosingResponse} sem
     *  obrigar o consumidor a conhecê-lo. Reaproveitado como corpo de {@link #setClosing}: mesmo
     *  shape ({@code {"period": "yyyy-MM"}}). */
    @NullMarked
    private record ClosingDto(@Nullable String period) {}

    /** Projeção de {@code AccountResponse} — mesmo shape JSON, sem obrigar o consumidor a conhecer o
     *  modelo {@code Account}/{@code CreditCard} de {@code f002}/{@code f003}. */
    @NullMarked
    public record AccountView(
            UUID id,
            String name,
            String type,
            String color,
            boolean active,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay,
            BigDecimal currentBalance,
            List<CardView> cards
    ) implements View {
        /** Espelha {@code AccountResponse.Card} — projeção somente-leitura de cartão embutida na conta. */
        @NullMarked
        public record CardView(UUID id, String last4, UUID accountId, boolean active) {}
    }

    /** Corpo de {@link #createAccount}/{@link #updateAccount} — espelha {@code AccountRequest}. */
    @NullMarked
    public record AccountBody(
            String name,
            String type,
            String color,
            boolean active,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay
    ) {}

    /** Espelha {@code BalanceResponse}. */
    @NullMarked
    public record BalanceView(UUID accountId, YearMonth period, BigDecimal balance) implements View {}

    private static HTTPApi internalApi() {
        return Context.get(HTTPApi.class);
    }

    // ── Contas ──────────────────────────────────────────────────────

    public List<AccountView> accounts() {
        return List.of(internalApi().get("/accounts", AccountView[].class));
    }

    public AccountView account(UUID id) {
        return internalApi().get("/accounts/" + id, AccountView.class);
    }

    public AccountView createAccount(AccountBody body) {
        return internalApi().post("/accounts", body, AccountView.class);
    }

    public AccountView updateAccount(UUID id, AccountBody body) {
        return internalApi().patch("/accounts/" + id, body, AccountView.class);
    }

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@code MOVE} exige {@code targetId}. */
    public void deleteAccount(UUID id, @Nullable String strategy, @Nullable UUID targetId) {
        internalApi().delete("/accounts/" + id + deletionQuery(strategy, targetId));
    }

    // ── Saldos ──────────────────────────────────────────────────────

    /** Saldo do período para todas as contas da pessoa numa só chamada. */
    public List<BalanceView> balances(YearMonth period) {
        return List.of(internalApi().get("/accounts/balance?period=" + periodQs(period), BalanceView[].class));
    }

    public BalanceView monthlyBalance(UUID accountId, YearMonth period) {
        return internalApi().get("/accounts/" + accountId + "/balance?period=" + periodQs(period), BalanceView.class);
    }

    public List<BalanceView> yearBalances(UUID accountId, int year) {
        return List.of(internalApi().get("/accounts/" + accountId + "/balance?year=" + year, BalanceView[].class));
    }

    // ── Fechamento ─────────────────────────────────────────────────

    /** Período de fechamento contábil vigente ({@code yyyy-MM}) ou {@code null} quando não há. */
    public ClosedPeriod closingPeriod() {
        val result = internalApi().get("/accounts/closing", ClosingDto.class).period();
        return ClosedPeriod.of(result);
    }

    public ClosedPeriod setClosing(String period) {
        val result = internalApi().post("/accounts/closing", new ClosingDto(period), ClosingDto.class).period();
        return ClosedPeriod.of(result);
    }

    public void clearClosing() {
        internalApi().delete("/accounts/closing");
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static String periodQs(YearMonth period) {
        return period.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private static String deletionQuery(@Nullable String strategy, @Nullable UUID targetId) {
        if (strategy == null) return "";
        return targetId == null ? "?strategy=" + strategy : "?strategy=" + strategy + "&targetId=" + targetId;
    }
}
