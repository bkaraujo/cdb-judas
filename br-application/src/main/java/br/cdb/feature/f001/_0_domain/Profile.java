package br.cdb.feature.f001._0_domain;

import br.cdb.core.security.User;
import br.cdb.feature.f000._0_domain.model.Person;
import org.jspecify.annotations.NullMarked;

/**
 * Visão combinada da fatia {@code self} (/api/me): identidade/nome de exibição
 * ({@link Person}, do contexto people) e {@link Preferences} (feature), reunidas para
 * projeção de leitura/escrita. Não inclui o {@code username} de login ({@link User}) — a
 * fatia {@code profile} não conhece o agregado {@code User}; o {@code username} é resolvido
 * à parte por {@code SelfResource} na borda HTTP, que compõe os dois em {@code SelfView}.
 */
@NullMarked
public record Profile(Person person, Preferences preferences) {}
