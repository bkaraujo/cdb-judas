package br.community.feature.user.accounts.transactions.importer;

import br.commons.Logger;
import br.commons.Result;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.accounts.statement.Issuer;
import br.community.feature.user.accounts.statement.MonetaryDocument;
import br.community.feature.user.accounts.statement.MonetaryDocumentEntry;
import br.community.feature.user.accounts.statement.StatementParser;
import br.community.feature.user.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.community.feature.user.accounts.transactions.importer.preview.*;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates credit-card statement import via the monetary context facade.
 * Category assignment is done by the feature layer on top of USER_TRANSACTION (not here).
 */
@NullMarked
public class StatementImportUseCase {

    private static final int RECONCILE_WINDOW_DAYS = 3;

    private final MonetaryContext monetaryContext;
    private final CreditCardProvider creditCardProvider;
    private final PdfTextExtractor extractor;
    private final List<StatementParser> parsers;
    private final CardMatcher cardMatcher = new CardMatcher();
    private final InstallmentExpander expander;
    private final GroupSignature groupSignature = new GroupSignature();
    private final CategoryGuesser categoryGuesser = new CategoryGuesser();
    private final Clock clock;
    private final long maxFileBytes;

    public StatementImportUseCase(MonetaryContext monetaryContext, CreditCardProvider creditCardProvider, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes) {
        this(monetaryContext, creditCardProvider, extractor, parsers, bytes, Clock.system(ZoneId.systemDefault()));
    }

    public StatementImportUseCase(MonetaryContext monetaryContext, CreditCardProvider creditCardProvider, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes, Clock clock) {
        this.monetaryContext = monetaryContext;
        this.creditCardProvider = creditCardProvider;
        this.extractor = extractor;
        this.expander = new InstallmentExpander(groupSignature);
        this.parsers = parsers;
        this.maxFileBytes = bytes;
        this.clock = clock;
    }

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

    public Result<ImportResult, DomainError> confirm(ImportConfirmCommand cmd) {
        return resolveAccountsByCard(cmd).map(accountByCard -> {
            val today = LocalDate.now(clock);
            val seen = Collections.unmodifiableList(monetaryContext.listTransactions().getOrElse(List.of()));
            val existingGroups = seen.stream()
                    .map(Transaction::groupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            val saved = new ArrayList<Transaction>();
            val installments = persistInstallments(partitionInstallments(cmd, accountByCard), accountByCard, today, existingGroups, saved);
            val avista = persistAvista(cmd, accountByCard, today, seen, saved);

            return new ImportResult(installments.created() + avista.created(), 0, installments.skipped() + avista.skipped());
        });
    }

    private Result<Map<UUID, UUID>, DomainError> resolveAccountsByCard(ImportConfirmCommand cmd) {
        val linkedByCard = creditCardProvider.creditCards().stream()
                .filter(card -> card.linkedAccountId() != null)
                .collect(Collectors.toMap(CreditCard::id, card -> card.linkedAccountId()));
        val accountByCard = new HashMap<UUID, UUID>();
        for (val cardId : cmd.rows().stream().map(ImportConfirmCommand.Row::cardId).distinct().toList()) {
            switch (monetaryContext.findAccount(cardId)) {
                case Result.Success(var ignored) -> accountByCard.put(cardId, linkedByCard.getOrDefault(cardId, cardId));
                case Result.Failure(var error) -> { return new Result.Failure<>(error); }
            }
        }
        return new Result.Success<>(accountByCard);
    }

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

    private Counts persistInstallments(Map<UUID, List<ImportConfirmCommand.Row>> installmentByGroup,
                                       Map<UUID, UUID> accountByCard, LocalDate today,
                                       Set<UUID> existingGroups, List<Transaction> saved) {
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

    private Counts persistAvista(ImportConfirmCommand cmd, Map<UUID, UUID> accountByCard, LocalDate today,
                                 List<Transaction> seen, List<Transaction> saved) {
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
    private record Counts(int created, int skipped) {}

    private static UUID accountOf(Map<UUID, UUID> accountByCard, ImportConfirmCommand.Row row) {
        return Objects.requireNonNull(accountByCard.get(row.cardId()));
    }

    // ── Bank-statement path ────────────────────────────────────────

    public Result<ImportResult, DomainError> confirmStatement(BankStatementConfirmCommand cmd) {
        return monetaryContext.findAccount(cmd.accountId()).map(account -> {
            val accountId = account.id();
            val today = LocalDate.now(clock);

            val accountTx = monetaryContext.listTransactions().getOrElse(List.of()).stream()
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
                            monetaryContext.updateTransactionStatus(target.id(), Transaction.Status.CONFIRMED, row.date());
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
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Transaction.Status.SCHEDULED : Transaction.Status.CONFIRMED;
        val type = row.type() != null ? row.type() : (row.amount().signum() < 0 ? Transaction.Type.EXPENSE : Transaction.Type.INCOME);
        val command = new ImportedTransactionCommand(
                accountId, row.description(), row.amount(), row.date(),
                status, type, null, null, null);
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

    private static boolean isAvistaDuplicate(ImportConfirmCommand.Row row, UUID accountId, List<Transaction> seen) {
        val desc = GroupSignature.normalize(row.description());
        return seen.stream().anyMatch(t ->
                accountId.equals(t.accountId())
                        && row.date().equals(t.date())
                        && t.amount().abs().compareTo(row.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalize(t.description())));
    }

    @Nullable
    private Transaction persist(ImportConfirmCommand.Row row, UUID accountId, LocalDate today,
                                        @Nullable UUID groupId, @Nullable Integer installmentNumber,
                                        @Nullable Integer totalInstallments
    ) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Transaction.Status.SCHEDULED : Transaction.Status.CONFIRMED;
        val command = new ImportedTransactionCommand(
                accountId, row.description(), row.amount(), row.date(),
                status, Transaction.Type.EXPENSE, groupId, installmentNumber, totalInstallments);
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

    private Result<ImportPreviewOutcome, ImportError> preview(Issuer issuer, List<MonetaryDocumentEntry> statement, @Nullable UUID accountId) {
        val candidates = monetaryContext.listAccounts().getOrElse(List.of()).stream()
                .filter(a -> a.type() != Account.Type.CREDIT_CARD && a.active())
                .toList();
        val selectedAccountId = selectAccount(accountId, candidates);

        val history = monetaryContext.listTransactions().getOrElse(List.of());
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

    private static @Nullable UUID selectAccount(@Nullable UUID accountId, List<Account> candidates) {
        if (accountId != null) {
            return accountId;
        }
        return candidates.size() == 1 ? candidates.getFirst().id() : null;
    }

    private Result<ImportPreviewOutcome, ImportError> preview(Issuer issuer, List<MonetaryDocumentEntry> statement) {
        val last4s = statement.stream().map(MonetaryDocumentEntry::last4)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        val cards = creditCardProvider.creditCards().stream()
                .filter(card -> card.linkedAccountId() != null)
                .filter(card -> card.last4() != null && last4s.contains(card.last4()))
                .toList();

        val cardByLast4 = cardMatcher.matchByLast4(last4s, cards);

        val today = LocalDate.now(clock);
        val history = monetaryContext.listTransactions().getOrElse(List.of());

        val rows = new ArrayList<PreviewRow>();
        for (val line : statement) {
            val suggestedCard = line.last4() != null ? cardByLast4.get(line.last4()) : null;
            val accountId = resolveAccountId(suggestedCard);
            val suggestedCardId = suggestedCard != null ? suggestedCard.id() : null;
            for (val draft : expander.expand(line, accountId, today)) {
                val dup = suggestedCard != null && isDuplicate(draft, history);
                rows.add(new PreviewRow(draft, dup, null, suggestedCardId));
            }
        }

        return Result.success(new ImportPreviewOutcome.Invoice(
                new ImportPreview(issuer, statement, cards, List.copyOf(rows)))
        );
    }

    private static UUID resolveAccountId(@Nullable CreditCard card) {
        if (card == null) {
            return new UUID(0L, 0L);
        }
        return accountIdOf(card);
    }

    private static UUID accountIdOf(CreditCard card) {
        return card.linkedAccountId() != null ? card.linkedAccountId() : card.id();
    }

    private boolean isDuplicate(TransactionDraft draft, List<Transaction> existing) {
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
