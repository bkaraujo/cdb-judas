package br.cdb.feature.f001._2_infrastructure;

import br.cdb.feature.f001._0_domain.Profile;
import br.cdb.feature.user.UserUseCase;
import org.jspecify.annotations.NullMarked;

/**
 * Composição do recurso {@code self} (/api/me) usada pelo {@link UserUseCase}: junta o
 * {@link Profile} da fatia {@code profile} (Person + Preferences) com o {@code username} de
 * login, resolvido à parte via {@code UserRepository} — a fatia {@code profile} não conhece o
 * agregado {@code User}, então essa junção mora aqui, no único use case da fatia {@code user}.
 */
@NullMarked
public record SelfView(Profile profile, String username) {}
