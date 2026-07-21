package br.cdb.feature.f001._2_infrastructure;

import br.cdb.feature.f001._0_domain.Profile;
import org.jspecify.annotations.NullMarked;

/**
 * Composição do recurso {@code self} (/api/me): junta o {@link Profile} da fatia {@code profile}
 * (Person + Preferences) com o {@code username} de login, resolvido à parte via
 * {@code UserRepository} — a fatia {@code profile} não conhece o agregado {@code User}, então essa
 * junção mora aqui, em {@code SelfResource}.
 */
@NullMarked
public record SelfView(Profile profile, String username) {}
