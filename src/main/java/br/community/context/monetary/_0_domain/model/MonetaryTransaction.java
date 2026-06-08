package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record MonetaryTransaction(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        UUID accountId,
        String status,
        String type,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable String notes
) {}
