package br.cdb.feature.f008._2_infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record TagRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color
) {}
