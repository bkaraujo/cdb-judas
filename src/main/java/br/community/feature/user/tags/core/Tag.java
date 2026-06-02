package br.community.feature.user.tags.core;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record Tag(
        UUID id,
        String name,
        String color
) {}
