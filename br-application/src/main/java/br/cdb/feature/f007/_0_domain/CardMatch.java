package br.cdb.feature.f007._0_domain;

import br.cdb.context.monetary._0_domain.model.CreditCard;
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
    record Matched(CreditCard card) implements CardMatch {}

    @NullMarked
    record NoMatch() implements CardMatch {}

    @NullMarked
    record Ambiguous(List<CreditCard> candidates) implements CardMatch {}
}
