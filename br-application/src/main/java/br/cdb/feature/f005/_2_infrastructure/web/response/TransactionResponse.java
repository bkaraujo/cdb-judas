package br.cdb.feature.f005._2_infrastructure.web.response;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        @Nullable UUID categoryId,
        UUID accountId,
        Transaction.Status status,
        Transaction.Type type,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable String notes,
        @Nullable UUID cardId
) {}
