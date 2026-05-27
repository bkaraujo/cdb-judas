package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record Tag(
        UUID id,
        String name,
        String color
) {}
