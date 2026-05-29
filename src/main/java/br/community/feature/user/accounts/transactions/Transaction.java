package br.community.feature.user.accounts.transactions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record Transaction(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        UUID accountId,
        String status,
        String type,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments
) {}
