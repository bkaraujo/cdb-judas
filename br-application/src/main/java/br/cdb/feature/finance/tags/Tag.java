package br.cdb.feature.finance.tags;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record Tag(
        UUID id,
        String name,
        String color
) {}
