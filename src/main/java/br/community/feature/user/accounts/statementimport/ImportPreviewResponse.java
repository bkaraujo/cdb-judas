package br.community.feature.user.accounts.statementimport;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Preview payload: detected issuer, the distinct card last4s found on the statement, the expanded
 * charge rows (one per à-vista charge, {@code N} per parcelado charge), the pre-selected matched card
 * (null when none/ambiguous) and the registered credit cards offered for manual pick. {@code date}
 * and {@code originalDate} are absolute ISO {@code yyyy-MM-dd}; installment fields are null for
 * à-vista rows; {@code status} is {@code "confirmed"} or {@code "scheduled"}; {@code duplicate} flags
 * a row already imported on the matched card.
 */
@NullMarked
public record ImportPreviewResponse(
        String documentType,
        String issuer,
        List<String> last4s,
        List<Row> rows,
        @Nullable UUID matchedCardId,
        List<CardOption> candidateCards) {

    @NullMarked
    public record Row(
            String last4,
            String date,
            String originalDate,
            String description,
            BigDecimal amount,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            String status,
            boolean duplicate,
            @Nullable UUID categoryId) {}

    @NullMarked
    public record CardOption(UUID id, String name, @Nullable String last4) {}
}
