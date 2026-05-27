package br.community.context.monetary._1_application.command;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CostCenterCommand(
        @NotBlank String description
) {}
