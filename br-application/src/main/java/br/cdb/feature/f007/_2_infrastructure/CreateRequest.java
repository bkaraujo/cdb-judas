package br.cdb.feature.f007._2_infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record CreateRequest(
        @NotBlank String name,
        @NotBlank String nature,
        @Nullable UUID parentId
) {}
