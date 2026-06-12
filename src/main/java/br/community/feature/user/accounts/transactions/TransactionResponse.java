package br.community.feature.user.accounts.transactions;

import br.community.context.monetary._0_domain.model.MonetaryTransaction;
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
        UUID categoryId,
        UUID accountId,
        MonetaryTransaction.Status status,
        MonetaryTransaction.Type type,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable String notes
) {}
