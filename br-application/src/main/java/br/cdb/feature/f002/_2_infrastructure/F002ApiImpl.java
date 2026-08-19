package br.cdb.feature.f002._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f002._2_infrastructure.web.AccountBalanceResource;
import br.cdb.feature.f002._2_infrastructure.web.AccountResource;
import br.cdb.feature.f002._2_infrastructure.web.ClosingResource;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
 * <p>Context-wired ({@code Context.get(F002Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F002ApiImpl extends AbstractApiClient implements F002Api {

    // ── Contas ──────────────────────────────────────────────────────

    @Override
    public List<AccountView> accounts() {
        return list("/accounts", AccountView[].class);
    }

    @Override
    public AccountView account(UUID id) {
        return get("/accounts/" + id, AccountView.class);
    }

    @Override
    public AccountView createAccount(AccountBody body) {
        return post("/accounts", body, AccountView.class);
    }

    @Override
    public AccountView updateAccount(UUID id, AccountBody body) {
        return patch("/accounts/" + id, body, AccountView.class);
    }

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@code MOVE} exige {@code targetId}. */
    @Override
    public void deleteAccount(UUID id, @Nullable String strategy, @Nullable UUID targetId) {
        delete("/accounts/" + id + deletionQuery(strategy, targetId));
    }

    // ── Saldos ──────────────────────────────────────────────────────

    /** Saldo do período para todas as contas da pessoa numa só chamada. */
    @Override
    public List<BalanceView> balances(YearMonth period) {
        return list("/accounts/balance?period=" + periodQs(period), BalanceView[].class);
    }

    @Override
    public BalanceView monthlyBalance(UUID accountId, YearMonth period) {
        return get("/accounts/" + accountId + "/balance?period=" + periodQs(period), BalanceView.class);
    }

    @Override
    public List<BalanceView> yearBalances(UUID accountId, int year) {
        return list("/accounts/" + accountId + "/balance?year=" + year, BalanceView[].class);
    }

    // ── Fechamento ─────────────────────────────────────────────────


    /** Corpo mínimo do endpoint {@code GET /accounts/closing} — espelha {@code ClosingResponse} sem
     *  obrigar o consumidor a conhecê-lo. Reaproveitado como corpo de {@link #setClosing}: mesmo
     *  shape ({@code {"period": "yyyy-MM"}}). */
    @NullMarked
    private record ClosingDto(@Nullable String period) {}

    /** Período de fechamento contábil vigente ({@code yyyy-MM}) ou {@code null} quando não há. */
    @Override
    public ClosedPeriod closingPeriod() {
        val result = get("/accounts/closing", ClosingDto.class).period();
        return ClosedPeriod.of(result);
    }

    @Override
    public ClosedPeriod setClosing(String period) {
        val result = post("/accounts/closing", new ClosingDto(period), ClosingDto.class).period();
        return ClosedPeriod.of(result);
    }

    @Override
    public void clearClosing() {
        delete("/accounts/closing");
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
