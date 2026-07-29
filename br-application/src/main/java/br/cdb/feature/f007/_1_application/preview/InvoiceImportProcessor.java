package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f007._0_domain.*;
import br.cdb.feature.f007._1_application.CardMatcher;
import br.cdb.feature.f007._1_application.GroupSignature;
import br.cdb.feature.f007._1_application.InstallmentExpander;
import br.cdb.feature.f007._1_application.confirm.InvoiceConfirmCommand;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes {@link br.cdb.feature.f007._0_domain.MonetaryDocument.Invoice} documents (credit-card
 * statements): preview (card match + installment expansion + dedup) and confirm (persistence of
 * kept rows, grouped by installment schedule). Every persisted transaction also gets its
 * {@code PERSON_TRANSACTION} overlay here, keeping the 1:1 invariant with {@code MON_TRANSACTION}.
 */
@NullMarked
public class InvoiceImportProcessor {

    private final TransactionUseCase ucTransaction = Registry.tryGet(TransactionUseCase.class);

    private final CreditCardProvider creditCardProvider;
    private final CardMatcher cardMatcher = new CardMatcher();
    private final GroupSignature groupSignature = new GroupSignature();
    private final InstallmentExpander expander;
    private final TransactionOverlaySink transactionOverlaySink;
    private final Clock clock;

    public InvoiceImportProcessor(CreditCardProvider creditCardProvider, TransactionOverlaySink transactionOverlaySink, Clock clock) {
        this.creditCardProvider = creditCardProvider;
        this.expander = new InstallmentExpander(groupSignature);
        this.transactionOverlaySink = transactionOverlaySink;
        this.clock = clock;
    }

    public Result<ImportPreviewOutcome, ImportError> preview(String issuer, @Nullable YearMonth period, List<MonetaryDocumentEntry> statement) {
        val last4s = statement.stream().map(MonetaryDocumentEntry::last4)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        val cards = creditCardProvider.creditCards().stream()
                .filter(card -> last4s.contains(card.last4()))
                .toList();

        val cardByLast4 = cardMatcher.matchByLast4(last4s, cards);

        val today = LocalDate.now(clock);
        val statementPeriod = period != null ? period : YearMonth.from(today);
        val history = ucTransaction.transactions().getOrElse(List.of());

        val rows = new ArrayList<PreviewRow>();
        for (val line : statement) {
            val suggestedCard = line.last4() != null ? cardByLast4.get(line.last4()) : null;
            val accountId = resolveAccountId(suggestedCard);
            val suggestedCardId = suggestedCard != null ? suggestedCard.id() : null;
            for (val draft : expander.expand(line, accountId, statementPeriod, today)) {
                val dup = suggestedCard != null && isDuplicate(draft, history);
                Logger.verbose("Attaching %s", draft);
                rows.add(new PreviewRow(draft, dup, null, suggestedCardId));
            }
        }

        return Result.success(new ImportPreviewOutcome.Invoice(
                new ImportPreview(issuer, statement, cards, List.copyOf(rows)))
        );
    }

    public Result<ImportResult, BusinessError> confirm(UUID personId, InvoiceConfirmCommand cmd) {
        return resolveAccountsByCard(cmd).map(accountByCard -> {
            val today = LocalDate.now(clock);
            val seen = Collections.unmodifiableList(ucTransaction.transactions().getOrElse(List.of()));
            val existingGroups = seen.stream()
                    .map(Transaction::groupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            val saved = new ArrayList<Transaction>();
            val installments = persistInstallments(personId, partitionInstallments(cmd, accountByCard), accountByCard, today, existingGroups, saved);
            val avista = persistAvista(personId, cmd, accountByCard, today, seen, saved);

            return new ImportResult(installments.created() + avista.created(), 0, installments.skipped() + avista.skipped());
        });
    }

    private Result<Map<UUID, UUID>, BusinessError> resolveAccountsByCard(InvoiceConfirmCommand cmd) {
        val accountByCardId = creditCardProvider.creditCards().stream()
                .collect(Collectors.toMap(CreditCard::id, CreditCard::accountId));
        val accountByCard = new HashMap<UUID, UUID>();
        for (val cardId : cmd.rows().stream().map(InvoiceConfirmCommand.Row::cardId).distinct().toList()) {
            val accountId = accountByCardId.get(cardId);
            if (accountId == null) {
                return new Result.Failure<>(new BusinessError.NotFound("CreditCard not found: " + cardId));
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

    private static boolean isAvistaDuplicate(InvoiceConfirmCommand.Row row, UUID accountId, List<Transaction> seen) {
        val desc = GroupSignature.normalize(row.description());
        return seen.stream().anyMatch(t ->
                accountId.equals(t.accountId())
                        && row.date().equals(t.date())
                        && t.amount().abs().compareTo(row.amount().abs()) == 0
                        && desc.equals(GroupSignature.normalize(t.description())));
    }

    @Nullable
    private Transaction persist(UUID personId, InvoiceConfirmCommand.Row row, UUID accountId, LocalDate today,
                                @Nullable UUID groupId, @Nullable Integer installmentNumber,
                                @Nullable Integer totalInstallments
    ) {
        val status = YearMonth.from(row.date()).isAfter(YearMonth.from(today)) ? Transaction.Status.SCHEDULED : Transaction.Status.CONFIRMED;
        val tx = new Transaction(
                UUID.randomUUID(), row.description(), row.amount(), row.date(),
                accountId, status, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null,
                groupId, installmentNumber == null ? 1 : installmentNumber,
                totalInstallments == null ? 1 : totalInstallments, null, row.cardId());
        try {
            return switch (ucTransaction.create(tx)) {
                case Result.Success(var saved) -> {
                    transactionOverlaySink.save(saved.id(), saved.accountId(), personId, row.categoryId());
                    yield saved;
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

    private static UUID resolveAccountId(@Nullable CreditCard creditCard) {
        return creditCard != null ? creditCard.accountId() : new UUID(0L, 0L);
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
