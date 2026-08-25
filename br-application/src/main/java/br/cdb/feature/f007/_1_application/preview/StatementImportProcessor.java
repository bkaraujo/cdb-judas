package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f000._0_domain.event.TransactionImported;
import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f007._0_domain.model.*;
import br.cdb.feature.f007._1_application.TransactionWriter;
import br.cdb.feature.f007._1_application.confirm.StatementConfirmCommand;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Processes {@link MonetaryDocument.Statement} documents
 * (checking-account extracts): preview (account selection + dedup/reconcile classification) and
 * confirm (persistence of new rows, reconciliation of matched pending/scheduled transactions). Toda
 * leitura cross-slice (contas, histórico) é via {@link F002Api}/{@link F006Api} (D1 de
 * {@code .claude/plan.md}); toda escrita é via a porta {@link TransactionWriter}, resolvida por
 * chamada (nunca em campo — ver seu javadoc). Every persisted transaction publishes
 * {@code TransactionImported} (f000) — o listener de overlay (em {@code f006}, {@code F006Module})
 * grava o vínculo {@code F006_TRANSACTION_CATEGORY}, mantendo o 1:1 com {@code F006_TRANSACTION}.
 */
@NullMarked
public class StatementImportProcessor {

    private static final int RECONCILE_WINDOW_DAYS = 3;

    private final Clock clock;

    public StatementImportProcessor(Clock clock) {
        this.clock = clock;
    }

    private static TransactionWriter writer() {
        return Context.get(TransactionWriter.class);
    }

    public Result<ImportPreviewOutcome, ImportError> preview(String personId, String issuer, List<MonetaryDocumentEntry> statement, @Nullable UUID accountId) {
        val candidates = Context.get(F002Api.class).accounts().stream()
                .filter(F002Api.AccountView::active)
                .toList();
        val selectedAccountId = selectAccount(accountId, candidates, Strings.lower(issuer));

        val history = Context.get(F006Api.class).transactions();
        val accountTx = selectedAccountId != null
                ? history.stream().filter(t -> selectedAccountId.equals(t.accountId())).toList()
                : List.<F006Api.TransactionView>of();

        val classes = classify(statement, accountTx);

        val rows = new ArrayList<BankStatementPreviewRow>();
        for (int i = 0; i < statement.size(); i++) {
            rows.add(bankRow(statement.get(i), classes.get(i)));
        }

        return Result.success(new ImportPreviewOutcome.Statement(
                new BankStatementPreview(issuer, candidates, selectedAccountId, List.copyOf(rows)))
        );
    }

    public Result<ImportResult, BusinessError> confirmStatement(UUID personId, StatementConfirmCommand cmd) {
        // Ownership de cmd.accountId() já foi validada na entrada da fatia (ImportUseCase.guards.ownsAccount)
        // antes de chegar aqui — sem checagem redundante por HTTP.
        val accountId = cmd.accountId();
        val today = LocalDate.now(clock);

        val accountTx = Context.get(F006Api.class).transactions().stream()
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
                        writer().confirmStatus(target.id(), row.date());
                        MessageBus.submit(new TransactionImported(target.id(), target.accountId(), personId, row.categoryId(), row.tagIds()));
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

        return Result.success(new ImportResult(created, reconciled, skipped));
    }

    private boolean persistStatementRow(UUID personId, StatementConfirmCommand.Row row, UUID accountId, LocalDate today) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Status.SCHEDULED : Status.CONFIRMED;
        val type = row.type() != null ? row.type() : (row.amount().signum() < 0 ? Nature.EXPENSE : Nature.INCOME);
        val planned = row.planned();
        val imported = new ImportedTransaction(row.description(), row.amount(), row.date(), accountId, status, type,
                planned, null, null, null, null);
        try {
            return switch (writer().create(imported)) {
                case Result.Success(var id) -> {
                    MessageBus.submit(new TransactionImported(id, accountId, personId, row.categoryId(), row.tagIds()));
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

    /** {@code categoryId} e {@code planned} saem sempre nulos/padrão: quem escolhe cada linha é o
     *  usuário (ou uma regra de nomenclatura casada api-side), na tela. */
    private BankStatementPreviewRow bankRow(MonetaryDocumentEntry line, Classification cls) {
        val type = line.amount().signum() < 0 ? Nature.EXPENSE : Nature.INCOME;
        val reconcileDescription = cls.target() != null ? cls.target().description() : null;
        return new BankStatementPreviewRow(
                line.date(), line.description(), line.amount(), type, cls.state(), null, null, reconcileDescription);
    }

    private List<Classification> classify(List<MonetaryDocumentEntry> movements, List<F006Api.TransactionView> accountTx) {
        val consumed = new HashSet<UUID>();
        val out = new ArrayList<Classification>();
        for (val mv : movements) {
            val desc = GroupSignature.normalize(mv.description());

            val dup = accountTx.stream()
                    .filter(t -> !consumed.contains(t.id()))
                    .filter(t -> mv.date().equals(t.date())
                            && t.amount().compareTo(mv.amount()) == 0
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
                    .filter(t -> t.amount().compareTo(mv.amount()) == 0)
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

    private static boolean isReconcilable(Status status) {
        return Status.PENDING.equals(status) || Status.SCHEDULED.equals(status);
    }

    @NullMarked
    private record Classification(RowState state, F006Api.@Nullable TransactionView target) {}

    private static @Nullable UUID selectAccount(@Nullable UUID accountId, List<F002Api.AccountView> candidates, String issuer) {
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
