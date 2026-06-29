package br.community.feature.user.profile;

import br.community.core.web.security.User;
import org.jspecify.annotations.NullMarked;

/**
 * Visão combinada da fatia {@code self} (/api/me): identidade ({@link User}, do core) e
 * {@link Preferences} (feature), reunidas para projeção de leitura/escrita.
 */
@NullMarked
public record Profile(User user, Preferences preferences) {}
