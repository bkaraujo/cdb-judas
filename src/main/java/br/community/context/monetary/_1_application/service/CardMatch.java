package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
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
    record Matched(MonetaryAccount card) implements CardMatch {}

    @NullMarked
    record NoMatch() implements CardMatch {}

    @NullMarked
    record Ambiguous(List<MonetaryAccount> candidates) implements CardMatch {}
}
