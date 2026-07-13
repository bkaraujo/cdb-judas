package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.feature.user.accounts.transactions.importer.CardMatch;
import br.cdb.feature.user.accounts.transactions.importer.CardMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CreditCardMatcherTest {

    private final CardMatcher matcher = new CardMatcher();

    private static CreditCard card(String last4) {
        return new CreditCard(UUID.randomUUID(), last4, UUID.randomUUID(), true);
    }

    @Test
    void matchesSingleCardWhoseLast4IsOnStatement() {
        var visa = card("0020");
        var master = card("9999");

        var result = matcher.match(List.of("0020"), List.of(visa, master));

        var matched = assertInstanceOf(CardMatch.Matched.class, result);
        assertEquals(visa, matched.card());
    }

    @Test
    void noMatchWhenNoCardHasThatLast4() {
        var visa = card("0020");

        var result = matcher.match(List.of("1234"), List.of(visa));

        assertInstanceOf(CardMatch.NoMatch.class, result);
    }

    @Test
    void noMatchWhenStatementLast4sEmpty() {
        var visa = card("0020");

        var result = matcher.match(List.of(), List.of(visa));

        assertInstanceOf(CardMatch.NoMatch.class, result);
    }

    @Test
    void ambiguousWhenTwoCardsShareTheSameLast4() {
        var titular = card("0020");
        var adicional = card("0020");

        var result = matcher.match(List.of("0020"), List.of(titular, adicional));

        var ambiguous = assertInstanceOf(CardMatch.Ambiguous.class, result);
        assertEquals(List.of(titular, adicional), ambiguous.candidates());
    }

    // ── matchByLast4 (per-charge suggestion) ────────────────────────────────────

    @Test
    void matchByLast4ResolvesEachLast4ToItsOwnCard() {
        var visa = card("0020");
        var master = card("9999");

        var result = matcher.matchByLast4(List.of("0020", "9999"), List.of(visa, master));

        assertEquals(Map.of("0020", visa, "9999", master), result);
    }

    @Test
    void matchByLast4OmitsLast4MatchedByNoCard() {
        var visa = card("0020");

        var result = matcher.matchByLast4(List.of("0020", "1234"), List.of(visa));

        assertEquals(Map.of("0020", visa), result);
    }

    @Test
    void matchByLast4OmitsAmbiguousLast4() {
        var titular = card("0020");
        var adicional = card("0020");
        var other = card("9999");

        var result = matcher.matchByLast4(List.of("0020", "9999"), List.of(titular, adicional, other));

        // 0020 matched by two cards → omitted; 9999 uniquely matched → present.
        assertEquals(Map.of("9999", other), result);
    }
}
