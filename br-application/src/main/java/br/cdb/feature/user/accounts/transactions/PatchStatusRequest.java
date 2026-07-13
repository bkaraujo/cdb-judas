package br.cdb.feature.user.accounts.transactions;

import br.cdb.context.monetary._0_domain.model.Transaction;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

@NullMarked
public record PatchStatusRequest(
        @NotNull Transaction.Status status,
        @NotNull LocalDate paymentDate
) {}
