package br.community.context.monetary._1_application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record CardCommand(
        @NotNull UUID accountId,
        @NotBlank @Pattern(regexp = "\\d{4}") String last4
) {}
