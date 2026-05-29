package br.community.feature.user.accounts.transactions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

@NullMarked
public record PatchStatusRequest(
        @NotBlank String status,
        @NotNull LocalDate paymentDate
) {}