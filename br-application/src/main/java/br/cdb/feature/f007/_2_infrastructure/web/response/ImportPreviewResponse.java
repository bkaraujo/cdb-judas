package br.cdb.feature.f007._2_infrastructure.web.response;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Preview payload: detected issuer, the distinct card last4s found on the statement, the expanded
 * charge rows (one per à-vista charge, {@code N} per parcelado charge) and the registered credit cards
 * present on the statement, offered for per-row pick. {@code date} and {@code originalDate} are
 * absolute ISO {@code yyyy-MM-dd}; installment fields are null for à-vista rows; {@code status} is
 * {@code CONFIRMED} or {@code SCHEDULED}; {@code duplicate} flags a row already imported on its
 * suggested card.
 */
@NullMarked
public record ImportPreviewResponse(
        String documentType,
        String issuer,
        List<String> last4s,
        List<Row> rows,
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
            @Nullable UUID groupId,
            Transaction.Status status,
            boolean duplicate,
            @Nullable UUID categoryId,
            @Nullable UUID suggestedCardId) {}

    @NullMarked
    public record CardOption(UUID id, String name, @Nullable String last4) {}
}
