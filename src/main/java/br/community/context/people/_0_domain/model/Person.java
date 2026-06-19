package br.community.context.people._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Agregado de pessoa (identidade humana), inspirado no contexto {@code people} do jimmy. É o
 * "dono" de recursos como contas; um {@code User} (segurança) referencia uma {@code Person}.
 * {@code locale}/{@code language} em texto (ex.: "pt-BR"), em linha com o estilo do backend.
 */
@NullMarked
public record Person(
        UUID id,
        String name,
        String locale,
        String language
) {}
