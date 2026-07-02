package br.community.feature.user.accounts.cards;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardRequest(@NotBlank @Pattern(regexp = "\\d{4}") String last4) {}
