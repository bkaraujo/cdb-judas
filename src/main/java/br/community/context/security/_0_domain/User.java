package br.community.context.security._0_domain;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record User(
        String id,
        String username,
        String password
) { }
