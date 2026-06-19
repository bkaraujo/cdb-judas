package br.community.feature.user.accounts.transactions.importer;

import br.community.context.monetary._0_domain.model.Account;
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
    record Matched(Account card) implements CardMatch {}

    @NullMarked
    record NoMatch() implements CardMatch {}

    @NullMarked
    record Ambiguous(List<Account> candidates) implements CardMatch {}
}
