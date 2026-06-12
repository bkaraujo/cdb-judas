package br.community.feature.user.accounts.transactions;

import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

@NullMarked
public record PatchStatusRequest(
        @NotNull MonetaryTransaction.Status status,
        @NotNull LocalDate paymentDate
) {}
