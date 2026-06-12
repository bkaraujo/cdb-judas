package br.community.feature.user.accounts.transactions.importer;

import br.commons.Logger;
import br.commons.Result;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.AccountType;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.accounts.transactions.importer.preview.*;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the credit-card statement import as a feature-level use of the monetary context.
 * Wires PDF extraction → issuer detection → per-bank parser → card matching → installment expansion
 * → category guessing into a preview, and persists the kept rows on confirm. All access to the
 * monetary context goes through its {@link MonetaryContext} facade — this slice owns no persistence.
 */
@NullMarked
public class StatementImportUseCase {

    private static final int RECONCILE_WINDOW_DAYS = 3;

    private final MonetaryContext monetaryContext;
    private final PdfTextExtractor extractor;
    private final List<StatementParser> parsers;
    private final CardMatcher cardMatcher = new CardMatcher();
    private final InstallmentExpander expander;
    private final GroupSignature groupSignature = new GroupSignature();
    private final CategoryGuesser categoryGuesser = new CategoryGuesser();
    private final Clock clock;
    private final long maxFileBytes;

    public StatementImportUseCase(MonetaryContext monetaryContext, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes) {
        this(monetaryContext, extractor, parsers, bytes, Clock.systemDefaultZone());
    }

    /** Test seam: lets callers pin the clock so date-anchored parsing/status is deterministic. */
    public StatementImportUseCase(MonetaryContext monetaryContext, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes, Clock clock) {
        this.monetaryContext = monetaryContext;
        this.extractor = extractor;
        this.expander = new InstallmentExpander(groupSignature);
        this.parsers = parsers;
        this.maxFileBytes = bytes;
        this.clock = clock;
    }

    /**
     * Extracts the PDF text and routes by document type. {@code accountId} is honored only by the
     * bank-statement path — it lets the preview compute per-row duplicate/reconcile states against the
     * chosen destination account (the credit-card path matches its card by last4 instead).
     */
    public Result<ImportPreviewOutcome, ImportError> preview(byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (fileBytes.length > maxFileBytes) {
            return new Result.Failure<>(new ImportError.FileTooLarge(fileBytes.length, maxFileBytes));
        }

        return switch (extractor.extract(fileBytes, password)) {

            case Result.Success(var text) -> {
                if (text == null) yield new Result.Failure<>(new ImportError.NoTextLayer());

                val capable = parsers.stream().filter(parser -> parser.parseable(text)).toList();
                if (capable.size() != 1) {
                    yield new Result.Failure<>(new ImportError.UnknownIssuer());
                }

                yield switch (capable.getFirst().parse(text)) {
                    case MonetaryDocument.Invoice(var issuer, var statement) -> preview(issuer, statement);
                    case MonetaryDocument.Statement(var issuer, var statement) -> preview(issuer, statement, accountId);
                };
            }

            case Result.Failure(var failure) -> new Result.Failure<>(switch ((ExtractionFailure) failure) {
                case ExtractionFailure.Encrypted ignored -> new ImportError.PasswordRequired();
                case ExtractionFailure.WrongPassword ignored -> new ImportError.WrongPassword();
                case ExtractionFailure.NoTextLayer ignored -> new ImportError.NoTextLayer();
                case ExtractionFailure.TooManyPages(int pages, int maxPages) -> new ImportError.TooManyPages(pages, maxPages);
            });
        };
    }

    /**
     * Persists the kept rows on each row's own card via {@link MonetaryContext#createImportedTransaction},
     * recognizing already-imported charges so a re-import is idempotent. Parcelado rows are grouped by
     * their deterministic {@link GroupSignature#groupId} and skipped wholesale when that group already
     * exists; à-vista rows are deduped by account/date/amount/normalized-description. The facade emits
     * one transaction-created event per persisted row (drives balance recalc + SSE). Persistence is
     * best-effort: a failure on one row is logged and the loop continues — already-saved rows stand.
     * Fails fast when a row names an unknown card.
     */
    public Result<ImportResult, DomainError> confirm(ImportConfirmCommand cmd) {
        return resolveAccountsByCard(cmd).map(accountByCard -> {
            val today = LocalDate.now(clock);
            val seen = Collections.unmodifiableList(monetaryContext.listTransactions().getOrElse(List.of()));
            val existingGroups = seen.stream()
                    .map(MonetaryTransaction::groupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            val saved = new ArrayList<MonetaryTransaction>();
            val installments = persistInstallments(partitionInstallments(cmd, accountByCard), accountByCard, today, existingGroups, saved);
            val avista = persistAvista(cmd, accountByCard, today, seen, saved);

            return new ImportResult(installments.created() + avista.created(), 0, installments.skipped() + avista.skipped());
        });
    }

    /** Resolves every referenced card to its destination account up front, failing fast on an unknown
     *  card so a partial import never starts. The account drives dedup and group identity, so a mixed
     *  invoice spreads its rows across the right card accounts. */
    private Result<Map<UUID, UUID>, DomainError> resolveAccountsByCard(ImportConfirmCommand cmd) {
        val accountByCard = new HashMap<UUID, UUID>();
        for (val cardId : cmd.rows().stream().map(ImportConfirmCommand.Row::cardId).distinct().toList()) {
            switch (monetaryContext.findAccount(cardId)) {
                case Result.Success(var card) -> accountByCard.put(cardId, accountIdOf(card));
                case Result.Failure(var error) -> { return new Result.Failure<>(error); }
            }
        }
        return new Result.Success<>(accountByCard);
    }

    /** Groups the parcelado rows by their deterministic {@link GroupSignature#groupId}, preserving
     *  first-seen order; à-vista rows are left to {@link #persistAvista}. */
    private Map<UUID, List<ImportConfirmCommand.Row>> partitionInstallments(ImportConfirmCommand cmd, Map<UUID, UUID> accountByCard) {
        val installmentByGroup = new LinkedHashMap<UUID, List<ImportConfirmCommand.Row>>();
        for (val row : cmd.rows()) {
            if (row.installmentTotal() != null && row.installmentNumber() != null) {
                val accountId = accountOf(accountByCard, row);
                val groupId = groupSignature.groupId(accountId, row.originalDate(), row.installmentTotal(), row.description());
                installmentByGroup.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(row);
            }
        }
        return installmentByGroup;
    }

    /** Persists each new parcelado group on its card, skipping wholesale a group whose id already exists
     *  (re-import idempotence). Appends the saved rows to {@code saved} so the à-vista pass dedups
     *  against them. */
    private Counts persistInstallments(Map<UUID, List<ImportConfirmCommand.Row>> installmentByGroup,
                                       Map<UUID, UUID> accountByCard, LocalDate today,
                                       Set<UUID> existingGroups, List<MonetaryTransaction> saved) {
        val seenGroups = new HashSet<UUID>();
        int created = 0;
        int skipped = 0;
        for (val entry : installmentByGroup.entrySet()) {
            val groupId = entry.getKey();
            val group = entry.getValue();
            if (existingGroups.contains(groupId) || seenGroups.contains(groupId)) {
                skipped += group.size();
                continue;
            }
            for (val row : group) {
                val tx = persist(row, accountOf(accountByCard, row), today, groupId, row.installmentNumber(), row.installmentTotal());
                if (tx != null) {
                    saved.add(tx);
                    created++;
                }
            }
            seenGroups.add(groupId);
        }
        return new Counts(created, skipped);
    }

    /** Persists the à-vista rows (non-parcelado), deduping each by account/date/amount/normalized
     *  description against {@code seen} (the pre-existing transactions) plus {@code saved} (the rows
     *  persisted earlier in this run, including the parcelado ones). */
    private Counts persistAvista(ImportConfirmCommand cmd, Map<UUID, UUID> accountByCard, LocalDate today,
                                 List<MonetaryTransaction> seen, List<MonetaryTransaction> saved) {
        int created = 0;
        int skipped = 0;
        for (val row : cmd.rows()) {
            if (row.installmentTotal() != null && row.installmentNumber() != null) {
                continue;
            }
            val accountId = accountOf(accountByCard, row);
            if (isAvistaDuplicate(row, accountId, seen) || isAvistaDuplicate(row, accountId, saved)) {
                skipped++;
                continue;
            }
            val tx = persist(row, accountId, today, null, null, null);
            if (tx != null) {
                saved.add(tx);
                created++;
            }
        }
        return new Counts(created, skipped);
    }

    @NullMarked
    private record Counts(int created, int skipped) {
    }

    /** Destination account of a row's (required) card; membership is guaranteed by {@link #confirm}'s
     *  up-front resolution, so the lookup never misses. */
    private static UUID accountOf(Map<UUID, UUID> accountByCard, ImportConfirmCommand.Row row) {
        return Objects.requireNonNull(accountByCard.get(row.cardId()));
    }

    // ── Bank-statement path ────────────────────────────────────────

    /**
     * Persists the kept bank-statement rows on the chosen account. Each row's fate is re-derived
     * against the account's current transactions ({@link #classify}): an already-imported identical
     * row is skipped; a row that matches a pending/scheduled manual transaction promotes that one to
     * {@code confirmed} (reconciliation, no insert); otherwise a new transaction is created with the
     * sign-derived type. Best-effort: a failure on one row is logged and the loop continues.
     */
    public Result<ImportResult, DomainError> confirmStatement(BankStatementConfirmCommand cmd) {
        return monetaryContext.findAccount(cmd.accountId()).map(account -> {
            val accountId = account.id();
            val today = LocalDate.now(clock);

            val accountTx = monetaryContext.listTransactions().getOrElse(List.of()).stream()
                    .filter(t -> accountId.equals(t.accountId()))
                    .toList();

            val movements = cmd.rows().stream()
                    .map(r -> new ParsedStatementLine(r.date(), r.description(), r.amount()))
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
                            monetaryContext.updateTransactionStatus(target.id(), MonetaryTransaction.Status.CONFIRMED, row.date());
                            reconciled++;
                        }
                    }
                    case NEW -> {
                        if (persistStatementRow(row, accountId, today)) {
                            created++;
                        }
                    }
                }
            }

            return new ImportResult(created, reconciled, skipped);
        });
    }

    private boolean persistStatementRow(BankStatementConfirmCommand.Row row, UUID accountId, LocalDate today) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? MonetaryTransaction.Status.SCHEDULED : MonetaryTransaction.Status.CONFIRMED;
        val command = new ImportedTransactionCommand(
                accountId, row.description(), row.amount(), row.date(), row.categoryId(),
                status, row.type(), null, null, null);
        try {
            return switch (monetaryContext.createImportedTransaction(command)) {
                case Result.Success(var ignored) -> true;
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

    private BankStatementPreviewRow bankRow(ParsedStatementLine line, Classification cls,
                                            List<MonetaryTransaction> history, UUID fallbackCategoryId) {
        val type = line.amount().signum() < 0 ? MonetaryTransaction.Type.EXPENSE : MonetaryTransaction.Type.INCOME;
        val categoryId = cls.state() == RowState.NEW
                ? categoryGuesser.guess(line.description(), history).orElse(fallbackCategoryId)
                : null;
        val reconcileDescription = cls.target() != null ? cls.target().description() : null;
        return new BankStatementPreviewRow(
                line.date(), line.description(), line.amount(), type, cls.state(), categoryId, reconcileDescription);
    }

    /**
     * Classifies each statement movement against the chosen account's transactions, consuming each
     * existing transaction at most once (1:1): an identical already-imported row → DUPLICATE; a
     * pending/scheduled manual transaction with the same signed amount within ±{@value
     * #RECONCILE_WINDOW_DAYS} days (closest date wins) → RECONCILE; otherwise NEW.
     */
    private List<Classification> classify(List<ParsedStatementLine> movements, List<MonetaryTransaction> accountTx) {
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

    private static boolean isReconcilable(MonetaryTransaction.Status status) {
        return MonetaryTransaction.Status.PENDING.equals(status) || MonetaryTransaction.Status.SCHEDULED.equals(status);
    }

    @NullMarked
    private record Classification(RowState state, @Nullable MonetaryTransaction target) {
    }

    private static boolean isAvistaDuplicate(ImportConfirmCommand.Row row, UUID accountId, List<MonetaryTransaction> seen) {
        val desc = GroupSignature.normalize(row.description());
        return seen.stream().anyMatch(t ->
                accountId.equals(t.accountId())
                        && row.date().equals(t.date())
                        && t.amount().abs().compareTo(row.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalize(t.description())));
    }

    @Nullable
    private MonetaryTransaction persist(ImportConfirmCommand.Row row, UUID accountId, LocalDate today,
                                        @Nullable UUID groupId, @Nullable Integer installmentNumber,
                                        @Nullable Integer totalInstallments
    ) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? MonetaryTransaction.Status.SCHEDULED : MonetaryTransaction.Status.CONFIRMED;
        val command = new ImportedTransactionCommand(
                accountId, row.description(), row.amount(), row.date(), row.categoryId(),
                status, MonetaryTransaction.Type.EXPENSE, groupId, installmentNumber, totalInstallments);
        try {
            return switch (monetaryContext.createImportedTransaction(command)) {
                case Result.Success(var saved) -> saved;
                case Result.Failure(var error) -> {
                    Logger.warn("Failed to persist imported row '%s': %s", row.description(), String.valueOf(error));
                    yield null;
                }
            };
        } catch (RuntimeException e) {
            Logger.warn("Failed to persist imported row '%s': %s", row.description(), Strings.orEmpty(e.getMessage()));
            return null;
        }
    }

    private Result<ImportPreviewOutcome, ImportError> preview(Issuer issuer, List<ParsedStatementLine> statement, @Nullable UUID accountId) {
        val candidates = monetaryContext.listAccounts().getOrElse(List.of()).stream()
                .filter(a -> a.type() != AccountType.CREDIT_CARD && a.active())
                .toList();
        val selectedAccountId = selectAccount(accountId, candidates);

        val history = monetaryContext.listTransactions().getOrElse(List.of());
        val accountTx = selectedAccountId != null
                ? history.stream().filter(t -> selectedAccountId.equals(t.accountId())).toList()
                : Collections.unmodifiableList(new ArrayList<MonetaryTransaction>());
        val fallbackCategoryId = monetaryContext.findOrCreateUncategorizedCategory().id();

        val classes = classify(statement, accountTx);

        val rows = new ArrayList<BankStatementPreviewRow>();
        for (int i = 0; i < statement.size(); i++) {
            rows.add(bankRow(statement.get(i), classes.get(i), history, fallbackCategoryId));
        }

        return Result.success(new ImportPreviewOutcome.Statement(
                new BankStatementPreview(issuer, candidates, selectedAccountId, List.copyOf(rows)))
        );
    }

    /**
     * Sem conta escolhida, usa a única candidata elegível (quando há exatamente uma).
     */
    private static @Nullable UUID selectAccount(@Nullable UUID accountId, List<MonetaryAccount> candidates) {
        if (accountId != null) {
            return accountId;
        }
        return candidates.size() == 1 ? candidates.getFirst().id() : null;
    }

    private Result<ImportPreviewOutcome, ImportError> preview(Issuer issuer, List<ParsedStatementLine> statement) {
        // Only registered cards present on this statement are offered: linked to a bank account (so the
        // charges have a destination) and carrying a last4 printed on the invoice. The card is per row,
        // so an unmatched/ambiguous last4 still leaves every invoice card pickable.
        val last4s = statement.stream().map(ParsedStatementLine::last4)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        val cards = monetaryContext.listCreditCards().getOrElse(List.of()).stream()
                .filter(card -> card.linkedAccountId() != null)
                .filter(card -> last4s.contains(card.additionalInfo().getOrDefault("last4", Strings.EMPTY)))
                .toList();

        // Each charge's last4 is matched to a card individually, so an invoice mixing several cards
        // pre-selects the right card per row (the user can still override it on confirm).
        val cardByLast4 = cardMatcher.matchByLast4(last4s, cards);

        val today = LocalDate.now(clock);
        val history = monetaryContext.listTransactions().getOrElse(List.of());
        val fallbackCategoryId = monetaryContext.findOrCreateUncategorizedCategory().id();

        val rows = new ArrayList<PreviewRow>();
        for (val line : statement) {
            val suggestedCard = line.last4() != null ? cardByLast4.get(line.last4()) : null;
            val accountId = resolveAccountId(suggestedCard);
            val suggestedCardId = suggestedCard != null ? suggestedCard.id() : null;
            for (val draft : expander.expand(line, accountId, today)) {
                val dup = suggestedCard != null && isDuplicate(draft, history);
                val categoryId = categoryGuesser.guess(draft.description(), history).orElse(fallbackCategoryId);
                rows.add(new PreviewRow(draft, dup, categoryId, suggestedCardId));
            }
        }

        return Result.success(new ImportPreviewOutcome.Invoice(
                new ImportPreview(issuer, statement, cards, List.copyOf(rows)))
        );
    }

    /**
     * Conta de destino dos lançamentos do cartão: a conta vinculada, o próprio cartão, ou sentinela se não casou.
     */
    private static UUID resolveAccountId(@Nullable MonetaryAccount card) {
        if (card == null) {
            return new UUID(0L, 0L);
        }
        return accountIdOf(card);
    }

    private static UUID accountIdOf(MonetaryAccount card) {
        return card.linkedAccountId() != null ? card.linkedAccountId() : card.id();
    }

    private boolean isDuplicate(TransactionDraft draft, List<MonetaryTransaction> existing) {
        if (draft.groupId() != null) {
            return existing.stream().anyMatch(t -> draft.groupId().equals(t.groupId()));
        }
        val desc = GroupSignature.normalize(draft.description());
        return existing.stream().anyMatch(t ->
                draft.accountId().equals(t.accountId())
                        && draft.date().equals(t.date())
                        && t.amount().abs().compareTo(draft.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalize(t.description())));
    }
}
