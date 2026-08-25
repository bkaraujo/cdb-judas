package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f000._0_domain.event.TransactionImported;
import br.cdb.feature.f003.F003Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f007._0_domain.model.*;
import br.cdb.feature.f007._1_application.CreditCardProvider;
import br.cdb.feature.f007._1_application.TransactionWriter;
import br.cdb.feature.f007._1_application.confirm.InvoiceConfirmCommand;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes {@link MonetaryDocument.Invoice} documents (credit-card
 * statements): preview (card match + installment expansion + dedup) and confirm (persistence of
 * kept rows, grouped by installment schedule). Toda leitura cross-slice do histórico é via
 * {@link F006Api} (D1 de {@code .claude/plan.md}); toda escrita é via a porta
 * {@link TransactionWriter}, resolvida por chamada (nunca em campo — ver seu javadoc). Every
 * persisted transaction publishes {@code TransactionImported} (f000) — o listener de overlay (em
 * {@code f006}, {@code F006Module}) grava o vínculo {@code F006_TRANSACTION_CATEGORY}, mantendo o
 * 1:1 com {@code F006_TRANSACTION}.
 */
@NullMarked
public class InvoiceImportProcessor {


    private final CreditCardProvider creditCardProvider;
    private final CardMatcher cardMatcher = new CardMatcher();
    private final GroupSignature groupSignature = new GroupSignature();
    private final InstallmentExpander expander;
    private final Clock clock;

    public InvoiceImportProcessor(CreditCardProvider creditCardProvider, Clock clock) {
        this.creditCardProvider = creditCardProvider;
        this.expander = new InstallmentExpander(groupSignature);
        this.clock = clock;
    }

    private static TransactionWriter writer() {
        return Context.get(TransactionWriter.class);
    }

    public Result<ImportPreviewOutcome, ImportError> preview(String personId, String issuer, @Nullable YearMonth period,
                                                             List<MonetaryDocumentEntry> statement, Map<String, BigDecimal> printedTotals) {
        reconcile(printedTotals, statement);

        val last4s = statement.stream().map(MonetaryDocumentEntry::last4)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        val cards = creditCardProvider.creditCards().stream()
                .filter(card -> last4s.contains(card.last4()))
                .toList();

        val cardByLast4 = cardMatcher.matchByLast4(last4s, cards);

        val today = LocalDate.now(clock);
        val statementPeriod = period != null ? period : YearMonth.from(today);
        val history = Context.get(F006Api.class).transactions();

        val rows = new ArrayList<PreviewRow>();
        for (val line : statement) {
            val suggestedCard = line.last4() != null ? cardByLast4.get(line.last4()) : null;
            val accountId = resolveAccountId(suggestedCard);
            val suggestedCardId = suggestedCard != null ? suggestedCard.id() : null;
            for (val draft : expander.expand(line, accountId, statementPeriod, today)) {
                val dup = suggestedCard != null && isDuplicate(draft, history);
                Logger.verbose("Attaching %s", draft);
                rows.add(new PreviewRow(draft, dup, null, null, suggestedCardId));
            }
        }

        return Result.success(new ImportPreviewOutcome.Invoice(
                new ImportPreview(issuer, statement, cards, List.copyOf(rows)))
        );
    }

    /**
     * Logs (never blocks) a mismatch between each card's own printed checksum and what this slice
     * kept for it. Compares against the parser's raw {@code statement} entries — one per printed line,
     * each carrying exactly what the invoice charged that period — not the installment-expanded
     * drafts: a parcelado draft repeats the same amount N times (the reconstructed carnê), so summing
     * drafts would never match the invoice's own total by design.
     */
    private static void reconcile(Map<String, BigDecimal> printedTotals, List<MonetaryDocumentEntry> statement) {
        if (printedTotals.isEmpty()) {
            return;
        }
        val netByCard = new HashMap<String, BigDecimal>();
        for (val entry : statement) {
            if (entry.last4() == null) {
                continue;
            }
            val signed = entry.type() == Nature.INCOME ? entry.amount().negate() : entry.amount();
            netByCard.merge(entry.last4(), signed, BigDecimal::add);
        }
        for (val printed : printedTotals.entrySet()) {
            val computed = netByCard.getOrDefault(printed.getKey(), BigDecimal.ZERO);
            if (computed.compareTo(printed.getValue()) != 0) {
                Logger.warn("Invoice reconciliation mismatch for card final %s: printed=%s, computed=%s, diff=%s",
                        printed.getKey(), printed.getValue(), computed, printed.getValue().subtract(computed));
            }
        }
    }

    public Result<ImportResult, BusinessError> confirm(UUID personId, InvoiceConfirmCommand cmd) {
        return resolveAccountsByCard(cmd).map(accountByCard -> {
            val today = LocalDate.now(clock);
            val seen = Context.get(F006Api.class).transactions();
            val existingGroups = seen.stream()
                    .map(F006Api.TransactionView::groupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            val saved = new ArrayList<F006Api.TransactionView>();
            val installments = persistInstallments(personId, partitionInstallments(cmd, accountByCard), accountByCard, today, existingGroups, saved);
            val avista = persistAvista(personId, cmd, accountByCard, today, seen, saved);

            return new ImportResult(installments.created() + avista.created(), 0, installments.skipped() + avista.skipped());
        });
    }

    private Result<Map<UUID, UUID>, BusinessError> resolveAccountsByCard(InvoiceConfirmCommand cmd) {
        val accountByCardId = creditCardProvider.creditCards().stream()
                .collect(Collectors.toMap(F003Api.CardView::id, F003Api.CardView::accountId));
        val accountByCard = new HashMap<UUID, UUID>();
        for (val cardId : cmd.rows().stream().map(InvoiceConfirmCommand.Row::cardId).distinct().toList()) {
            val accountId = accountByCardId.get(cardId);
            if (accountId == null) {
                return new Result.Failure<>(new BusinessError.NotFound("f003.creditCard.notFound", cardId));
            }
            accountByCard.put(cardId, accountId);
        }
        return new Result.Success<>(accountByCard);
    }

    private Map<UUID, List<InvoiceConfirmCommand.Row>> partitionInstallments(InvoiceConfirmCommand cmd, Map<UUID, UUID> accountByCard) {
        val installmentByGroup = new LinkedHashMap<UUID, List<InvoiceConfirmCommand.Row>>();
        for (val row : cmd.rows()) {
            if (row.installmentTotal() != null && row.installmentNumber() != null) {
                val accountId = accountOf(accountByCard, row);
                val groupId = groupSignature.groupId(accountId, row.originalDate(), row.installmentTotal(), row.description());
                installmentByGroup.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(row);
            }
        }
        return installmentByGroup;
    }

    private Counts persistInstallments(UUID personId, Map<UUID, List<InvoiceConfirmCommand.Row>> installmentByGroup,
                                       Map<UUID, UUID> accountByCard, LocalDate today,
                                       Set<UUID> existingGroups, List<F006Api.TransactionView> saved) {
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
                val tx = persist(personId, row, accountOf(accountByCard, row), today, groupId, row.installmentNumber(), row.installmentTotal());
                if (tx != null) {
                    saved.add(tx);
                    created++;
                }
            }
            seenGroups.add(groupId);
        }
        return new Counts(created, skipped);
    }

    private Counts persistAvista(UUID personId, InvoiceConfirmCommand cmd, Map<UUID, UUID> accountByCard, LocalDate today,
                                 List<F006Api.TransactionView> seen, List<F006Api.TransactionView> saved) {
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
            val tx = persist(personId, row, accountId, today, null, null, null);
            if (tx != null) {
                saved.add(tx);
                created++;
            }
        }
        return new Counts(created, skipped);
    }

    @NullMarked
    private record Counts(int created, int skipped) {}

    private static UUID accountOf(Map<UUID, UUID> accountByCard, InvoiceConfirmCommand.Row row) {
        return Objects.requireNonNull(accountByCard.get(row.cardId()));
    }

    private static boolean isAvistaDuplicate(InvoiceConfirmCommand.Row row, UUID accountId, List<F006Api.TransactionView> seen) {
        val desc = GroupSignature.normalize(row.description());
        return seen.stream().anyMatch(t ->
                accountId.equals(t.accountId())
                        && row.date().equals(t.date())
                        && t.amount().abs().compareTo(row.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalize(t.description())));
    }

    private F006Api.@Nullable TransactionView persist(UUID personId, InvoiceConfirmCommand.Row row, UUID accountId, LocalDate today,
                                @Nullable UUID groupId, @Nullable Integer installmentNumber,
                                @Nullable Integer totalInstallments
    ) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Status.SCHEDULED : Status.CONFIRMED;
        val planned = row.planned();
        val type = row.type() != null ? row.type() : Nature.EXPENSE;
        val imported = new ImportedTransaction(row.description(), row.amount(), row.originalDate(), row.date(), accountId, status, type,
                planned, groupId, installmentNumber, totalInstallments, row.cardId());
        try {
            return switch (writer().create(imported)) {
                case Result.Success(var id) -> {
                    MessageBus.submit(new TransactionImported(id, accountId, personId, row.categoryId(), row.tagIds()));
                    yield new F006Api.TransactionView(id, accountId, row.description(), row.amount(), row.date(), status, type, groupId, row.cardId());
                }
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

    private static UUID resolveAccountId(F003Api.@Nullable CardView creditCard) {
        return creditCard != null ? creditCard.accountId() : new UUID(0L, 0L);
    }

    private boolean isDuplicate(TransactionDraft draft, List<F006Api.TransactionView> existing) {
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
