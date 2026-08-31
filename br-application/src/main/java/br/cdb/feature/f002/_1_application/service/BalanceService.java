package br.cdb.feature.f002._1_application.service;

import br.cdb.core.web.InternalCall;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._0_domain.service.InvoiceCycle;
import br.cdb.feature.f006.F006Api;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
public class BalanceService {

    private final BalanceRepository repository = Context.get(BalanceRepository.class);
    private final AccountService accountService = Context.tryGet(AccountService.class);
    private final F006Api f006Api = Context.tryGet(F006Api.class);

    public List<Balance> findByAccount(UUID accountId) {
        return repository.findByAccount(accountId);
    }

    public Result<Balance, BusinessError> findByAccountAndPeriod(UUID accountId, YearMonth period) {
        return repository.findByAccount(accountId).stream()
                .filter(b -> b.period().equals(period))
                .findFirst()
                .<Result<Balance, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("f002.balance.notFoundForPeriod", period)));
    }

    public List<Balance> findByAccountAndYear(UUID accountId, int year) {
        return repository.findByAccount(accountId).stream()
                .filter(b -> b.period().getYear() == year)
                .sorted(Comparator.comparing(Balance::period))
                .toList();
    }

    public Balance save(Balance balance) {
        return repository.save(balance);
    }

    public void deleteById(Balance balance) {
        repository.delete(balance.account().id(), balance.period());
    }

    public void markDirty(UUID accountId) {
        repository.markDirty(accountId);
    }

    /**
     * Recomputa toda conta com pelo menos um snapshot sujo — consumido pelo job de reconciliação
     * (f999) e pela varredura de boot ({@link #recomputeAll}). Falha de uma conta (ex.: loopback HTTP
     * de {@link #readTransactions} indisponível) não pode abortar as demais — cada conta roda isolada.
     */
    public void recomputeDirty() {
        repository.findDirtyAccountIds().forEach(accountId -> {
            try {
                recalculate(accountId);
            } catch (RuntimeException e) {
                Logger.warn("Falha ao recalcular saldo da conta %s: %s", accountId, e.toString());
            }
        });
    }

    /**
     * Varredura de startup: marca todo snapshot como sujo e recomputa na hora. Existe porque a
     * regra de cálculo pode mudar entre versões (foi o caso da fatura de cartão, que passou a
     * debitar no vencimento em vez da data da compra) sem que nenhuma transação seja escrita — sem
     * isto, {@code F002_BALANCE} ficaria com valores da regra antiga até a próxima escrita em cada
     * conta. Barato: recomputa a partir das transações que já estão em memória por conta.
     */
    public void recomputeAll() {
        repository.markAllDirty();
        recomputeDirty();
    }


    @NullMarked
    private record MonthBalance(
            LocalDate date,
            BigDecimal amount
    ) {}

    /**
     * Compra de cartão não sai da conta na data da compra — sai quando a fatura vence. Por isso a
     * transação com {@code cardId} é re-datada para {@link InvoiceCycle#dueDate}: o valor é o mesmo,
     * muda o mês do snapshot em que ele entra. Transação sem cartão conta na própria data.
     */
    public void recalculate(UUID accountId) {
        val accountResult = accountService.findById(accountId);
        if (accountResult.isFailure()) return;
        val account = accountResult.get();
        val transactions = readTransactions(account).stream()
                .map(t -> new MonthBalance(
                        t.cardId() == null ? t.date() : InvoiceCycle.dueDate(account, t.date()),
                        t.amount()))
                .toList();

        recalculateBalance(account, transactions);
    }

    /**
     * Roda também fora de requisição HTTP (recomputação de boot, job de reconciliação de {@code f999}),
     * e a leitura cross-slice é HTTP real — daí declarar o dono da conta como ator ({@link InternalCall}),
     * que é de onde sai o {@code personId} da rota e o token efêmero. Conta sem dono (fixture de teste,
     * linha antiga sem {@code COD_PERSON}) chama direto: quem resolve a pessoa aí é a requisição corrente.
     */
    private List<F006Api.TransactionView> readTransactions(Account account) {
        val personId = account.personId();
        if (personId == null) return f006Api.transactionsByAccount(account.id(), null, null, null);

        return InternalCall.as(personId, () -> f006Api.transactionsByAccount(account.id(), null, null, null));
    }

    private void recalculateBalance(Account account, List<MonthBalance> transactions) {
        val existingBalances = findByAccount(account.id());

        if (transactions.isEmpty()) {
            // No activity left → no monthly snapshots make sense; drop any stale rows so reads
            // fall back to the account's initial value.
            existingBalances.forEach(this::deleteById);
            return;
        }

        val firstMonth = transactions.stream()
                .map(t -> YearMonth.from(t.date()))
                .min(Comparator.naturalOrder())
                .orElse(YearMonth.now(ZoneId.systemDefault()));

        // Recompute the whole timeline from the first activity through the current month, so every
        // month up to today has a snapshot (months with no movement carry the prior value forward).
        var endMonth = YearMonth.now(ZoneId.systemDefault());
        for (val t : transactions) {
            val month = YearMonth.from(t.date());
            if (month.isAfter(endMonth)) endMonth = month;
        }
        for (val b : existingBalances) {
            if (b.period().isAfter(endMonth)) endMonth = b.period();
        }

        // Drop snapshots that precede all current activity (e.g. the earliest transactions were deleted).
        existingBalances.stream()
                .filter(b -> b.period().isBefore(firstMonth))
                .forEach(this::deleteById);

        Logger.verbose("Recalculating balance for account %s from %s to %s", account.id(), firstMonth, endMonth);

        var runningBalance = BigDecimal.ZERO;
        var current = firstMonth;
        while (!current.isAfter(endMonth)) {
            val finalCurrent = current;
            val monthSum = transactions.stream()
                    .filter(t -> YearMonth.from(t.date()).equals(finalCurrent))
                    .map(MonthBalance::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            runningBalance = runningBalance.add(monthSum);
            upsertBalance(account, current, runningBalance, existingBalances);
            current = current.plusMonths(1);
        }
    }

    private void upsertBalance(Account account, YearMonth period, BigDecimal balance, List<Balance> existing) {
        val existingOpt = existing.stream().filter(b -> b.period().equals(period)).findFirst();
        if (existingOpt.isPresent()) {
            val b = existingOpt.get();
            if (b.value().compareTo(balance) != 0) {
                save(new Balance(account, period, balance));
            }
        } else {
            save(new Balance(account, period, balance));
        }
    }
}
