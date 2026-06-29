package br.community.context.monetary;

import br.community.feature.user.accounts.transactions.importer.CardMatch;
import br.community.feature.user.accounts.transactions.importer.CardMatcher;
import br.community.feature.user.accounts.transactions.importer.CreditCard;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CardMatcherTest {

    private final CardMatcher matcher = new CardMatcher();

    private static CreditCard cardWithLast4(String name, String last4) {
        return new CreditCard(UUID.randomUUID(), name, last4, null);
    }

    private static CreditCard cardWithoutLast4(String name) {
        return new CreditCard(UUID.randomUUID(), name, null, null);
    }

    @Test
    void matchesSingleCardWhoseLast4IsOnStatement() {
        var visa = cardWithLast4("Visa", "0020");
        var master = cardWithLast4("Master", "9999");

        var result = matcher.match(List.of("0020"), List.of(visa, master));

        var matched = assertInstanceOf(CardMatch.Matched.class, result);
        assertEquals(visa, matched.card());
    }

    @Test
    void noMatchWhenNoCardHasThatLast4() {
        var visa = cardWithLast4("Visa", "0020");

        var result = matcher.match(List.of("1234"), List.of(visa));

        assertInstanceOf(CardMatch.NoMatch.class, result);
    }

    @Test
    void noMatchWhenStatementLast4sEmpty() {
        var visa = cardWithLast4("Visa", "0020");

        var result = matcher.match(List.of(), List.of(visa));

        assertInstanceOf(CardMatch.NoMatch.class, result);
    }

    @Test
    void noMatchWhenCardHasNoLast4() {
        var noLast4 = cardWithoutLast4("Sem final");

        var result = matcher.match(List.of("0020"), List.of(noLast4));

        assertInstanceOf(CardMatch.NoMatch.class, result);
    }

    @Test
    void ambiguousWhenTwoCardsShareTheSameLast4() {
        var titular = cardWithLast4("Titular", "0020");
        var adicional = cardWithLast4("Adicional", "0020");

        var result = matcher.match(List.of("0020"), List.of(titular, adicional));

        var ambiguous = assertInstanceOf(CardMatch.Ambiguous.class, result);
        assertEquals(List.of(titular, adicional), ambiguous.candidates());
    }

    // ── matchByLast4 (per-charge suggestion) ────────────────────────────────────

    @Test
    void matchByLast4ResolvesEachLast4ToItsOwnCard() {
        var visa = cardWithLast4("Visa", "0020");
        var master = cardWithLast4("Master", "9999");

        var result = matcher.matchByLast4(List.of("0020", "9999"), List.of(visa, master));

        assertEquals(Map.of("0020", visa, "9999", master), result);
    }

    @Test
    void matchByLast4OmitsLast4MatchedByNoCard() {
        var visa = cardWithLast4("Visa", "0020");

        var result = matcher.matchByLast4(List.of("0020", "1234"), List.of(visa));

        assertEquals(Map.of("0020", visa), result);
    }

    @Test
    void matchByLast4OmitsAmbiguousLast4() {
        var titular = cardWithLast4("Titular", "0020");
        var adicional = cardWithLast4("Adicional", "0020");
        var other = cardWithLast4("Other", "9999");

        var result = matcher.matchByLast4(List.of("0020", "9999"), List.of(titular, adicional, other));

        // 0020 matched by two cards → omitted; 9999 uniquely matched → present.
        assertEquals(Map.of("9999", other), result);
    }
}
