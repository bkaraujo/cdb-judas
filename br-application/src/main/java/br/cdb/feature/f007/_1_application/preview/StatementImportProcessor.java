package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f002._1_application.usecase.AccountUseCase;
import br.cdb.feature.f006._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f007._0_domain.ImportError;
import br.cdb.feature.f007._0_domain.ImportResult;
import br.cdb.feature.f007._0_domain.MonetaryDocumentEntry;
import br.cdb.feature.f007._0_domain.RowState;
import br.cdb.feature.f007._0_domain.TransactionOverlaySink;
import br.cdb.feature.f007._1_application.CategoryGuesser;
import br.cdb.feature.f007._1_application.GroupSignature;
import br.cdb.feature.f007._1_application.confirm.StatementConfirmCommand;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Processes {@link br.cdb.feature.f007._0_domain.MonetaryDocument.Statement} documents
 * (checking-account extracts): preview (account selection + dedup/reconcile classification) and
 * confirm (persistence of new rows, reconciliation of matched pending/scheduled transactions). Every
 * persisted transaction also gets its {@code PERSON_TRANSACTION} overlay here, keeping the 1:1
 * invariant with {@code MON_TRANSACTION}.
 */
@NullMarked
public class StatementImportProcessor {

    private static final int RECONCILE_WINDOW_DAYS = 3;

    private final AccountUseCase ucAccount = Registry.tryGet(AccountUseCase.class);
    private final TransactionUseCase ucTransaction = Registry.tryGet(TransactionUseCase.class);

    private final CategoryGuesser categoryGuesser = new CategoryGuesser();
    private final TransactionOverlaySink transactionOverlaySink;
    private final Clock clock;

    public StatementImportProcessor(TransactionOverlaySink transactionOverlaySink, Clock clock) {
        this.transactionOverlaySink = transactionOverlaySink;
        this.clock = clock;
    }

    public Result<ImportPreviewOutcome, ImportError> preview(String personId, String issuer, List<MonetaryDocumentEntry> statement, @Nullable UUID accountId) {
        val candidates = ucAccount.listAccounts(personId).getOrElse(List.of()).stream()
                .filter(Account::active)
                .toList();
        val selectedAccountId = selectAccount(accountId, candidates, Strings.lower(issuer));

        val history = ucTransaction.transactions(personId).getOrElse(List.of());
        val accountTx = selectedAccountId != null
                ? history.stream().filter(t -> selectedAccountId.equals(t.accountId())).toList()
                : Collections.unmodifiableList(new ArrayList<Transaction>());

        val classes = classify(statement, accountTx);

        val rows = new ArrayList<BankStatementPreviewRow>();
        for (int i = 0; i < statement.size(); i++) {
            rows.add(bankRow(statement.get(i), classes.get(i), List.of()));
        }

        return Result.success(new ImportPreviewOutcome.Statement(
                new BankStatementPreview(issuer, candidates, selectedAccountId, List.copyOf(rows)))
        );
    }

    public Result<ImportResult, BusinessError> confirmStatement(UUID personId, StatementConfirmCommand cmd) {
        val personIdStr = personId.toString();
        return ucAccount.findAccount(cmd.accountId(), personIdStr).map(account -> {
            val accountId = account.id();
            val today = LocalDate.now(clock);

            val accountTx = ucTransaction.transactions(personIdStr).getOrElse(List.of()).stream()
                    .filter(t -> accountId.equals(t.accountId()))
                    .toList();

            val movements = cmd.rows().stream()
                    .map(r -> new MonetaryDocumentEntry(r.date(), r.description(), r.amount()))
                    .toList();
            val classes = classify(movements, accountTx);

            int created = 0;
            int reconciled = 0;
            int skipped = 0;
            for (int i = 0; i < cmd.rows().size(); i++) {
                val row = cmd.rows().get(i);
                val cls = classes.get(i);
                switch (cls.state()) {
                    case DUPLICATE -> skipped++;
                    case RECONCILE -> {
                        val target = cls.target();
                        if (target != null) {
                            ucTransaction.updateTransactionStatus(target.id(), Transaction.Status.CONFIRMED, row.date());
                            reconciled++;
                        }
                    }
                    case NEW -> {
                        if (persistStatementRow(personId, row, accountId, today)) {
                            created++;
                        }
                    }
                }
            }

            return new ImportResult(created, reconciled, skipped);
        });
    }

    private boolean persistStatementRow(UUID personId, StatementConfirmCommand.Row row, UUID accountId, LocalDate today) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Transaction.Status.SCHEDULED : Transaction.Status.CONFIRMED;
        val type = row.type() != null ? row.type() : (row.amount().signum() < 0 ? Transaction.Type.EXPENSE : Transaction.Type.INCOME);
        val tx = new Transaction(
                UUID.randomUUID(), row.description(), row.amount(), row.date(),
                accountId, status, type, CostCenter.VARIAVEL.id(), null,
                null, 1, 1, null, null);
        try {
            return switch (ucTransaction.create(tx)) {
                case Result.Success(var saved) -> {
                    transactionOverlaySink.save(saved.id(), saved.accountId(), personId, row.categoryId());
                    yield true;
                }
                case Result.Failure(var error) -> {
                    Logger.warn("Failed to persist statement row '%s': %s", row.description(), String.valueOf(error));
                    yield false;
                }
            };
        } catch (RuntimeException e) {
            Logger.warn("Failed to persist statement row '%s': %s", row.description(), Strings.orEmpty(e.getMessage()));
            return false;
        }
    }

    private BankStatementPreviewRow bankRow(MonetaryDocumentEntry line, Classification cls, List<CategoryGuesser.Entry> historyEntries) {
        val type = line.amount().signum() < 0 ? Transaction.Type.EXPENSE : Transaction.Type.INCOME;
        val categoryId = cls.state() == RowState.NEW
                ? categoryGuesser.guess(line.description(), historyEntries).orElse(null)
                : null;
        val reconcileDescription = cls.target() != null ? cls.target().description() : null;
        return new BankStatementPreviewRow(
                line.date(), line.description(), line.amount(), type, cls.state(), categoryId, reconcileDescription);
    }

    private List<Classification> classify(List<MonetaryDocumentEntry> movements, List<Transaction> accountTx) {
        val consumed = new HashSet<UUID>();
        val out = new ArrayList<Classification>();
        for (val mv : movements) {
            val desc = GroupSignature.normalize(mv.description());

            val dup = accountTx.stream()
                    .filter(t -> !consumed.contains(t.id()))
                    .filter(t -> mv.date().equals(t.date())
                            && BigDecimal.valueOf(t.signal()).multiply(t.amount()).compareTo(mv.amount()) == 0
                            && desc.equals(GroupSignature.normalize(t.description())))
                    .findFirst();
            if (dup.isPresent()) {
                consumed.add(dup.get().id());
                out.add(new Classification(RowState.DUPLICATE, dup.get()));
                continue;
            }

            val rec = accountTx.stream()
                    .filter(t -> !consumed.contains(t.id()))
                    .filter(t -> isReconcilable(t.status()))
                    .filter(t -> BigDecimal.valueOf(t.signal()).multiply(t.amount()).compareTo(mv.amount()) == 0)
                    .filter(t -> Math.abs(ChronoUnit.DAYS.between(t.date(), mv.date())) <= RECONCILE_WINDOW_DAYS)
                    .min(Comparator.comparingLong(t -> Math.abs(ChronoUnit.DAYS.between(t.date(), mv.date()))));
            if (rec.isPresent()) {
                consumed.add(rec.get().id());
                out.add(new Classification(RowState.RECONCILE, rec.get()));
                continue;
            }

            out.add(new Classification(RowState.NEW, null));
        }
        return out;
    }

    private static boolean isReconcilable(Transaction.Status status) {
        return Transaction.Status.PENDING.equals(status) || Transaction.Status.SCHEDULED.equals(status);
    }

    @NullMarked
    private record Classification(RowState state, @Nullable Transaction target) {}

    private static @Nullable UUID selectAccount(@Nullable UUID accountId, List<Account> candidates, String issuer) {
        if (accountId != null) {
            return accountId;
        }
        if (candidates.size() == 1) {
            return candidates.getFirst().id();
        }
        // Account não guarda banco/emissor — na ausência de accountId explícito, o único sinal
        // disponível é o nome da conta citar o issuer detectado no PDF (ex.: "Conta BTG").
        // Só decide quando é inequívoco; com 0 ou 2+ nomes batendo, o usuário escolhe manualmente.
        val byName = candidates.stream()
                .filter(a -> Strings.lower(a.name()).contains(issuer))
                .toList();
        return byName.size() == 1 ? byName.getFirst().id() : null;
    }
}
