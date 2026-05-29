package br.community.feature.user.accounts.statementimport;

import br.commons.Logger;
import br.commons.Result;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
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

    private final MonetaryContext monetaryContext;
    private final PdfTextExtractor extractor;
    private final IssuerDetector issuerDetector;
    private final CreditCardStatementParserRegistry parsers;
    private final CardMatcher cardMatcher;
    private final InstallmentExpander expander;
    private final GroupSignature groupSignature;
    private final CategoryGuesser categoryGuesser;
    private final Clock clock;
    private final long maxFileBytes;

    public Result<ImportPreview, ImportError> preview(byte[] fileBytes, @Nullable String password) {
        if (fileBytes.length > maxFileBytes) {
            return new Result.Failure<>(new ImportError.FileTooLarge(fileBytes.length, maxFileBytes));
        }

        return switch (extractor.extract(fileBytes, password)) {
            case Result.Failure(var failure) -> new Result.Failure<>(toImportError(failure));
            case Result.Success(var text) -> previewFromText(text);
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

            return new ImportResult(created, skipped);
        });
    }

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
                status, groupId, installmentNumber, totalInstallments);
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

    private Result<ImportPreview, ImportError> previewFromText(String text) {
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
        final UUID accountId = matchedCard != null
                ? (matchedCard.linkedAccountId() != null ? matchedCard.linkedAccountId() : matchedCard.id())
                : new UUID(0L, 0L);

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

        return Result.success(new ImportPreview(issuer, statement, matchedCard, cards, List.copyOf(rows)));
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
