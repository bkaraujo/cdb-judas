package br.cdb.context.monetary;

import br.cdb.feature.finance.accounts.transactions.importer.GroupSignature;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GroupSignatureTest {

    private final GroupSignature signature = new GroupSignature();

    @Test
    void groupIdExcludesAmountSoRoundingDriftDoesNotSplitGroups() {
        var accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var date = LocalDate.of(2024, 8, 3);

        // Santander spreads rounding cents: 231,81 one month, 231,79 the next — same purchase.
        var idA = signature.groupId(accountId, date, 10, "DECATHLON");
        var idB = signature.groupId(accountId, date, 10, "DECATHLON");

        assertEquals(idA, idB, "group personId must be identical when only the amount would differ");
    }

    @Test
    void groupIdStableForSameInputs() {
        var accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var date = LocalDate.of(2025, 1, 15);

        assertEquals(
                signature.groupId(accountId, date, 12, "  netflix  brasil "),
                signature.groupId(accountId, date, 12, "NETFLIX BRASIL"),
                "normalization makes equivalent descriptions hash the same");
    }

    @Test
    void groupIdDiffersWhenAnchoringFieldsDiffer() {
        var accountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var date = LocalDate.of(2025, 1, 15);

        assertNotEquals(
                signature.groupId(accountId, date, 12, "NETFLIX"),
                signature.groupId(accountId, date, 6, "NETFLIX"),
                "different installment totals are different purchases");
        assertNotEquals(
                signature.groupId(accountId, date, 12, "NETFLIX"),
                signature.groupId(accountId, date.plusDays(1), 12, "NETFLIX"),
                "different original dates are different purchases");
    }

    @Test
    void avistaKeyComposesFieldsWithNormalizedAmountAndDescription() {
        var accountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        var date = LocalDate.of(2025, 3, 9);

        var key = signature.avistaKey(accountId, date, new BigDecimal("60.00"), "  Microsoft  Store ");

        assertEquals(accountId + "|2025-03-09|60|MICROSOFT STORE", key);
    }

    @Test
    void avistaKeyIgnoresTrailingZeroDifferencesInAmount() {
        var accountId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        var date = LocalDate.of(2025, 3, 9);

        assertEquals(
                signature.avistaKey(accountId, date, new BigDecimal("60.00"), "MICROSOFT"),
                signature.avistaKey(accountId, date, new BigDecimal("60.0"), "MICROSOFT"));
    }

    @Test
    void normalizeCollapsesWhitespaceAndUppercases() {
        assertEquals("AMAZONMKTPLC MEGABYTEM",
                GroupSignature.normalize("  Amazonmktplc   Megabytem \t"));
    }
}
