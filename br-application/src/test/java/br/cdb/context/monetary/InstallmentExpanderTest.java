package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.finance.accounts.statement.MonetaryDocumentEntry;
import br.cdb.feature.finance.accounts.transactions.core.ChargeKind;
import br.cdb.feature.finance.accounts.transactions.importer.GroupSignature;
import br.cdb.feature.finance.accounts.transactions.importer.InstallmentExpander;
import br.cdb.feature.finance.accounts.transactions.importer.TransactionDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InstallmentExpanderTest {

    private final InstallmentExpander expander = new InstallmentExpander(new GroupSignature());
    private final UUID accountId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static MonetaryDocumentEntry avista(MonthDay date, BigDecimal amount, String description) {
        return MonetaryDocumentEntry.charge("0020", date, description, amount, ChargeKind.PURCHASE);
    }

    private static MonetaryDocumentEntry parcelado(MonthDay date, BigDecimal amount, String description, int n, int total) {
        return MonetaryDocumentEntry.charge("0020", date, description, amount, n, total, ChargeKind.PURCHASE);
    }

    @Test
    void avistaExpandsToSingleDraftWithoutInstallmentFields() {
        var today = LocalDate.of(2025, 7, 15);
        // "09 Mar" with anchor 2025-07 → diff 3-7=-4 (not < -6) → year 2025 → 2025-03-09.
        var drafts = expander.expand(avista(MonthDay.of(3, 9), new BigDecimal("60.00"), "Microsoft"), accountId, today);

        assertEquals(1, drafts.size());
        TransactionDraft d = drafts.getFirst();
        assertEquals(Transaction.Type.EXPENSE, d.type());
        assertNull(d.installmentNumber());
        assertNull(d.installmentTotal());
        assertNull(d.groupId());
        assertEquals(LocalDate.of(2025, 3, 9), d.date());
        assertEquals(d.originalDate(), d.date(), "à-vista date equals its original date");
        assertEquals(Transaction.Status.CONFIRMED, d.status());
    }

    @Test
    void parceladoExpandsToFullScheduleSharingOneGroupId() {
        var today = LocalDate.of(2025, 7, 15);
        // "15 Jul", 9/10 → anchor 2025-07 - 8 = 2024-11; diff 7-11=-4 → year 2024 → original 2024-07-15.
        var drafts = expander.expand(
                parcelado(MonthDay.of(7, 15), new BigDecimal("72.99"), "Amazonmktplc Megabytem", 9, 10), accountId, today);

        assertEquals(10, drafts.size());
        var originalDate = LocalDate.of(2024, 7, 15);
        for (int k = 1; k <= 10; k++) {
            TransactionDraft d = drafts.get(k - 1);
            assertEquals(k, d.installmentNumber());
            assertEquals(10, d.installmentTotal());
            assertEquals(originalDate.plusMonths(k - 1L), d.date(), "installment k dated originalDate + (k-1) months");
            assertEquals(originalDate, d.originalDate());
            assertEquals(Transaction.Type.EXPENSE, d.type());
        }
        Set<UUID> groupIds = drafts.stream().map(TransactionDraft::groupId).collect(Collectors.toSet());
        assertEquals(1, groupIds.size(), "all installments share one group personId");
    }

    @Test
    void statusSplitsByMonthRelativeToToday() {
        var today = LocalDate.of(2025, 7, 15);
        // 1/4 of "15 Jun": original 2025-06-15; installments Jun/Jul/Aug/Sep 2025.
        var drafts = expander.expand(
                parcelado(MonthDay.of(6, 15), new BigDecimal("100.00"), "Curso", 1, 4), accountId, today);

        assertEquals(4, drafts.size());
        assertEquals(Transaction.Status.CONFIRMED, drafts.get(0).status(), "Jun 2025 < today month → confirmed");
        assertEquals(Transaction.Status.CONFIRMED, drafts.get(1).status(), "Jul 2025 == today month → confirmed");
        assertEquals(Transaction.Status.SCHEDULED, drafts.get(2).status(), "Aug 2025 > today month → scheduled");
        assertEquals(Transaction.Status.SCHEDULED, drafts.get(3).status(), "Sep 2025 > today month → scheduled");
    }

    @Test
    void groupIdSurvivesNextInstallmentAcrossMonths() {
        // Same printed day/month, same account/description/total — but observed in consecutive statements.
        var printed = MonthDay.of(6, 3);
        var line2of3 = parcelado(printed, new BigDecimal("231.81"), "DECATHLON", 2, 3);
        var line3of3 = parcelado(printed, new BigDecimal("231.79"), "DECATHLON", 3, 3); // rounding drift

        // 2/3 seen in July's statement; 3/3 seen one month later in August's statement.
        var drafts2 = expander.expand(line2of3, accountId, LocalDate.of(2025, 7, 20));
        var drafts3 = expander.expand(line3of3, accountId, LocalDate.of(2025, 8, 20));

        UUID id2 = onlyGroupId(drafts2);
        UUID id3 = onlyGroupId(drafts3);
        assertEquals(id2, id3, "the group personId must be stable as n advances to n+1 across months");
    }

    private static UUID onlyGroupId(List<TransactionDraft> drafts) {
        Set<UUID> ids = drafts.stream().map(TransactionDraft::groupId).collect(Collectors.toSet());
        assertEquals(1, ids.size());
        UUID id = ids.iterator().next();
        assertTrue(id != null);
        return id;
    }
}
