package br.community.feature.operations.payables;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

@NullMarked
public record ConfirmRequest(@NotNull LocalDate paymentDate) {}
