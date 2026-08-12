package br.cdb.feature.f007._1_application;

import br.cdb.feature.f003.F003Api;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Outcome of matching a statement's card last4s against the registered credit cards: exactly one
 * registered card matched ({@link Matched}), none matched ({@link NoMatch}), or several matched
 * ({@link Ambiguous}) — the latter prompts a manual pick in the frontend.
 */
@NullMarked
public sealed interface CardMatch {

    @NullMarked
    record Matched(F003Api.CardView card) implements CardMatch {}

    @NullMarked
    record NoMatch() implements CardMatch {}

    @NullMarked
    record Ambiguous(List<F003Api.CardView> candidates) implements CardMatch {}
}
