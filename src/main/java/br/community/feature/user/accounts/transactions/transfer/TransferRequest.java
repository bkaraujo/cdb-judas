package br.community.feature.user.accounts.transactions.transfer;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record TransferRequest(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @NotNull LocalDate date,
        @NotNull @TwoDecimalPlaces BigDecimal amount
) {}
