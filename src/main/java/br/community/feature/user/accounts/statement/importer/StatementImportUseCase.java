package br.community.feature.user.accounts.statement.importer;

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
import br.community.feature.user.accounts.statement.importer.confirm.BankStatementConfirmCommand;
import br.community.feature.user.accounts.statement.importer.confirm.ImportResult;
import br.community.feature.user.accounts.statement.importer.preview.*;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class StatementImportUseCase {

    private static final int RECONCILE_WINDOW_DAYS = 3;

    private final MonetaryContext monetaryContext;
    private final PdfTextExtractor extractor;
    private final DocumentTypeDetector documentTypeDetector;
    private final IssuerDetector issuerDetector;
    private final CreditCardStatementParserRegistry parsers;
    private final BankStatementParserRegistry bankParsers;
    private final CardMatcher cardMatcher;
    private final InstallmentExpander expander;
    private final GroupSignature groupSignature;
    private final CategoryGuesser categoryGuesser;
    private final Clock clock;
    private final long maxFileBytes;

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
            case Result.Failure(var failure) -> new Result.Failure<>(toImportError(failure));
            case Result.Success(var text) -> previewFromText(text, accountId);
        };
    }

    /**
     * Persists the kept rows on the matched card via {@link MonetaryContext#createImportedTransaction},
     * recognizing already-imported charges so a re-import is idempotent. Parcelado rows are grouped by
     * their deterministic {@link GroupSignature#groupId} and skipped wholesale when that group already
     * exists; à-vista rows are deduped by account/date/amount/normalized-description. The facade emits
     * one transaction-created event per persisted row (drives balance recalc + SSE). Persistence is
     * best-effort: a failure on one row is logged and the loop continues — already-saved rows stand.
     */
    public Result<ImportResult, DomainError> confirm(ImportConfirmCommand cmd) {
        return monetaryContext.findAccount(cmd.cardId()).map(card -> {
            final UUID accountId = card.linkedAccountId() != null ? card.linkedAccountId() : card.id();
            final LocalDate today = LocalDate.now(clock);

            final List<MonetaryTransaction> allTx = monetaryContext.listTransactions().getOrElse(List.of());
            final List<MonetaryTransaction> seen = allTx.stream()
                    .filter(t -> accountId.equals(t.accountId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            final Set<UUID> existingGroups = allTx.stream()
                    .map(MonetaryTransaction::groupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            final Set<UUID> seenGroups = new HashSet<>();

            int created = 0;
            int skipped = 0;

            final List<ImportConfirmCommand.Row> avista = new ArrayList<>();
            final Map<UUID, List<ImportConfirmCommand.Row>> parceladoByGroup = new LinkedHashMap<>();
            for (val row : cmd.rows()) {
                if (row.installmentTotal() != null && row.installmentNumber() != null) {
                    final UUID groupId = groupSignature.groupId(
                            accountId, row.originalDate(), row.installmentTotal(), row.description());
                    parceladoByGroup.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(row);
                } else {
                    avista.add(row);
                }
            }

            for (val entry : parceladoByGroup.entrySet()) {
                final UUID groupId = entry.getKey();
                final List<ImportConfirmCommand.Row> group = entry.getValue();
                if (existingGroups.contains(groupId) || seenGroups.contains(groupId)) {
                    skipped += group.size();
                    continue;
                }
                for (val row : group) {
                    final MonetaryTransaction saved = persist(
                            row, accountId, today, groupId, row.installmentNumber(), row.installmentTotal());
                    if (saved != null) {
                        seen.add(saved);
                        created++;
                    }
                }
                seenGroups.add(groupId);
            }

            for (val row : avista) {
                if (isAvistaDuplicate(row, accountId, seen)) {
                    skipped++;
                    continue;
                }
                final MonetaryTransaction saved = persist(row, accountId, today, null, null, null);
                if (saved != null) {
                    seen.add(saved);
                    created++;
                }
            }

            return new ImportResult(created, 0, skipped);
        });
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
            final UUID accountId = account.id();
            final LocalDate today = LocalDate.now(clock);

            final List<MonetaryTransaction> accountTx = monetaryContext.listTransactions().getOrElse(List.of()).stream()
                    .filter(t -> accountId.equals(t.accountId()))
                    .toList();

            final List<ParsedBankStatementLine> movements = cmd.rows().stream()
                    .map(r -> new ParsedBankStatementLine(r.date(), r.description(), r.amount()))
                    .toList();
            final List<Classification> classes = classify(movements, accountTx);

            int created = 0;
            int reconciled = 0;
            int skipped = 0;
            for (int i = 0; i < cmd.rows().size(); i++) {
                final BankStatementConfirmCommand.Row row = cmd.rows().get(i);
                final Classification cls = classes.get(i);
                switch (cls.state()) {
                    case DUPLICATE -> skipped++;
                    case RECONCILE -> {
                        final MonetaryTransaction target = cls.target();
                        if (target != null) {
                            monetaryContext.updateTransactionStatus(target.id(), "confirmed", row.date());
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
        final String status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? "scheduled" : "confirmed";
        final ImportedTransactionCommand command = new ImportedTransactionCommand(
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

    private Result<ImportPreviewOutcome, ImportError> previewBankStatement(String text, @Nullable UUID accountId) {
        final Issuer issuer = issuerDetector.detect(text);
        final Optional<BankStatementParser> parser = bankParsers.forIssuer(issuer);
        if (parser.isEmpty()) {
            return new Result.Failure<>(new ImportError.UnknownIssuer());
        }
        final ParsedBankStatement statement = parser.get().parse(text);

        final List<MonetaryAccount> candidates = monetaryContext.listAccounts().getOrElse(List.of()).stream()
                .filter(a -> a.type() != AccountType.CREDIT_CARD && a.active())
                .toList();
        final @Nullable UUID selectedAccountId = selectAccount(accountId, candidates);

        final List<MonetaryTransaction> history = monetaryContext.listTransactions().getOrElse(List.of());
        final List<MonetaryTransaction> accountTx = selectedAccountId != null
                ? history.stream().filter(t -> selectedAccountId.equals(t.accountId())).toList()
                : List.of();
        final UUID fallbackCategoryId = monetaryContext.findOrCreateUncategorizedCategory().id();

        final List<Classification> classes = classify(statement.lines(), accountTx);

        final List<BankStatementPreviewRow> rows = new ArrayList<>();
        for (int i = 0; i < statement.lines().size(); i++) {
            rows.add(bankRow(statement.lines().get(i), classes.get(i), history, fallbackCategoryId));
        }

        return Result.success(new ImportPreviewOutcome.Statement(
                new BankStatementPreview(issuer, candidates, selectedAccountId, List.copyOf(rows))));
    }

    /** Sem conta escolhida, usa a única candidata elegível (quando há exatamente uma). */
    private static @Nullable UUID selectAccount(@Nullable UUID accountId, List<MonetaryAccount> candidates) {
        if (accountId != null) {
            return accountId;
        }
        return candidates.size() == 1 ? candidates.getFirst().id() : null;
    }

    private BankStatementPreviewRow bankRow(ParsedBankStatementLine line, Classification cls,
                                            List<MonetaryTransaction> history, UUID fallbackCategoryId) {
        final String type = line.amount().signum() < 0 ? "expense" : "income";
        final UUID categoryId = cls.state() == RowState.NEW
                ? categoryGuesser.guess(line.description(), history).orElse(fallbackCategoryId)
                : null;
        final String reconcileDescription = cls.target() != null ? cls.target().description() : null;
        return new BankStatementPreviewRow(
                line.date(), line.description(), line.amount(), type, cls.state(), categoryId, reconcileDescription);
    }

    /**
     * Classifies each statement movement against the chosen account's transactions, consuming each
     * existing transaction at most once (1:1): an identical already-imported row → DUPLICATE; a
     * pending/scheduled manual transaction with the same signed amount within ±{@value
     * #RECONCILE_WINDOW_DAYS} days (closest date wins) → RECONCILE; otherwise NEW.
     */
    private List<Classification> classify(List<ParsedBankStatementLine> movements, List<MonetaryTransaction> accountTx) {
        final Set<UUID> consumed = new HashSet<>();
        final List<Classification> out = new ArrayList<>();
        for (final ParsedBankStatementLine mv : movements) {
            final String desc = GroupSignature.normalizeDescription(mv.description());

            final Optional<MonetaryTransaction> dup = accountTx.stream()
                    .filter(t -> !consumed.contains(t.id()))
                    .filter(t -> mv.date().equals(t.date())
                            && t.amount().compareTo(mv.amount()) == 0
                            && desc.equals(GroupSignature.normalizeDescription(t.description())))
                    .findFirst();
            if (dup.isPresent()) {
                consumed.add(dup.get().id());
                out.add(new Classification(RowState.DUPLICATE, dup.get()));
                continue;
            }

            final Optional<MonetaryTransaction> rec = accountTx.stream()
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

    private static boolean isReconcilable(String status) {
        return "pending".equals(status) || "scheduled".equals(status);
    }

    @NullMarked
    private record Classification(RowState state, @Nullable MonetaryTransaction target) {}

    private static boolean isAvistaDuplicate(ImportConfirmCommand.Row row, UUID accountId, List<MonetaryTransaction> seen) {
        final String desc = GroupSignature.normalizeDescription(row.description());
        return seen.stream().anyMatch(t ->
                accountId.equals(t.accountId())
                        && row.date().equals(t.date())
                        && t.amount().abs().compareTo(row.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalizeDescription(t.description())));
    }

    @Nullable
    private MonetaryTransaction persist(ImportConfirmCommand.Row row, UUID accountId, LocalDate today,
                                        @Nullable UUID groupId, @Nullable Integer installmentNumber,
                                        @Nullable Integer totalInstallments) {
        final String status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? "scheduled" : "confirmed";
        final ImportedTransactionCommand command = new ImportedTransactionCommand(
                accountId, row.description(), row.amount(), row.date(), row.categoryId(),
                status, "expense", groupId, installmentNumber, totalInstallments);
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

    private Result<ImportPreviewOutcome, ImportError> previewFromText(String text, @Nullable UUID accountId) {
        return switch (documentTypeDetector.detect(text)) {
            case BANK_STATEMENT -> previewBankStatement(text, accountId);
            case CREDIT_CARD_INVOICE, UNKNOWN -> previewInvoice(text);
        };
    }

    private Result<ImportPreviewOutcome, ImportError> previewInvoice(String text) {
        final Issuer issuer = issuerDetector.detect(text);
        final Optional<CreditCardStatementParser> parser = parsers.forIssuer(issuer);
        if (parser.isEmpty()) {
            return new Result.Failure<>(new ImportError.UnknownIssuer());
        }
        final ParsedStatement statement = parser.get().parse(text);
        final List<MonetaryAccount> cards = monetaryContext.listCreditCards().getOrElse(List.of());
        final CardMatch match = cardMatcher.match(statement.last4s(), cards);
        final MonetaryAccount matchedCard = (match instanceof CardMatch.Matched(MonetaryAccount card)) ? card : null;

        final LocalDate today = LocalDate.now(clock);
        final UUID accountId = resolveAccountId(matchedCard);

        final List<MonetaryTransaction> history = monetaryContext.listTransactions().getOrElse(List.of());
        final List<MonetaryTransaction> existing = matchedCard != null
                ? history.stream().filter(t -> accountId.equals(t.accountId())).toList()
                : List.of();
        final UUID fallbackCategoryId = monetaryContext.findOrCreateUncategorizedCategory().id();

        final List<PreviewRow> rows = new ArrayList<>();
        for (var line : statement.lines()) {
            for (var draft : expander.expand(line, accountId, today)) {
                final boolean dup = matchedCard != null && isDuplicate(draft, existing);
                final UUID categoryId = categoryGuesser.guess(draft.description(), history).orElse(fallbackCategoryId);
                rows.add(new PreviewRow(draft, dup, categoryId));
            }
        }

        return Result.success(new ImportPreviewOutcome.Invoice(
                new ImportPreview(issuer, statement, matchedCard, cards, List.copyOf(rows))));
    }

    /** Conta de destino dos lançamentos do cartão: a conta vinculada, o próprio cartão, ou sentinela se não casou. */
    private static UUID resolveAccountId(@Nullable MonetaryAccount card) {
        if (card == null) {
            return new UUID(0L, 0L);
        }
        return card.linkedAccountId() != null ? card.linkedAccountId() : card.id();
    }

    private boolean isDuplicate(TransactionDraft draft, List<MonetaryTransaction> existing) {
        if (draft.groupId() != null) {
            return existing.stream().anyMatch(t -> draft.groupId().equals(t.groupId()));
        }
        final String desc = GroupSignature.normalizeDescription(draft.description());
        return existing.stream().anyMatch(t ->
                draft.accountId().equals(t.accountId())
                        && draft.date().equals(t.date())
                        && t.amount().abs().compareTo(draft.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalizeDescription(t.description())));
    }

    private static ImportError toImportError(ExtractionFailure failure) {
        return switch (failure) {
            case ExtractionFailure.Encrypted ignored -> new ImportError.PasswordRequired();
            case ExtractionFailure.WrongPassword ignored -> new ImportError.WrongPassword();
            case ExtractionFailure.NoTextLayer ignored -> new ImportError.NoTextLayer();
            case ExtractionFailure.TooManyPages(int pages, int maxPages) -> new ImportError.TooManyPages(pages, maxPages);
        };
    }
}
