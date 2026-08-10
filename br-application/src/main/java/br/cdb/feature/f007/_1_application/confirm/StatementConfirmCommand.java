package br.cdb.feature.f007._1_application.confirm;

import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm a bank-statement import. {@code type} may be null when the sign of {@code amount}
 * unambiguously determines the direction (positive = income, negative = expense).
 */
@NullMarked
public record StatementConfirmCommand(UUID accountId, List<Row> rows) {

    @NullMarked
    public record Row(
            String description,
            BigDecimal amount,
            LocalDate date,
            Transaction.@Nullable Type type,
            UUID categoryId,
            @Nullable UUID costCenterId,
            List<UUID> tagIds
    ) {}
}
