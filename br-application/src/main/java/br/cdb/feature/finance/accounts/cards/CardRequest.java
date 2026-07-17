package br.cdb.feature.finance.accounts.cards;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardRequest(@NotBlank @Pattern(regexp = "\\d{4}") String last4) {}
