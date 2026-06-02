package br.community.feature.user.categories.core;

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
