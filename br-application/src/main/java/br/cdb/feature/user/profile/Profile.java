package br.cdb.feature.user.profile;

import br.cdb.core.web.security.User;
import br.cdb.feature.user.profile.preference.Preferences;
import org.jspecify.annotations.NullMarked;

/**
 * Visão combinada da fatia {@code self} (/api/me): identidade ({@link User}, do core) e
 * {@link Preferences} (feature), reunidas para projeção de leitura/escrita.
 */
@NullMarked
public record Profile(User user, Preferences preferences) {}
