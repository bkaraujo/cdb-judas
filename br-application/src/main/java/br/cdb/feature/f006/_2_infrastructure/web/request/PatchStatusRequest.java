package br.cdb.feature.f006._2_infrastructure.web.request;

import br.cdb.feature.f006._0_domain.model.Transaction;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

@NullMarked
public record PatchStatusRequest(
        @NotNull Transaction.Status status,
        @NotNull LocalDate paymentDate
) {}
