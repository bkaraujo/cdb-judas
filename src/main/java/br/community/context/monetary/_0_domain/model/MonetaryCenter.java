package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record MonetaryCenter(
        UUID id,
        String description
) {}
