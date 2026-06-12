package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.feature.user.accounts.transactions.importer.preview.CategoryGuesser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryGuesserTest {

    private final CategoryGuesser guesser = new CategoryGuesser();

    private static MonetaryTransaction tx(String description, UUID categoryId, LocalDate date) {
        return new MonetaryTransaction(
                UUID.randomUUID(), description, new BigDecimal("-10.00"), date, categoryId,
                UUID.randomUUID(), "confirmed", "expense", MonetaryCenter.VARIAVEL_ID, null, null, null, null, null);
    }

    @Test
    void returnsMostFrequentCategoryAcrossNormalizedDescriptions() {
        var categoryX = UUID.randomUUID();
        var categoryY = UUID.randomUUID();
        var history = List.of(
                tx("IFOOD", categoryX, LocalDate.of(2024, 1, 1)),
                tx("ifood ", categoryX, LocalDate.of(2024, 2, 1)),
                tx(" iFood", categoryY, LocalDate.of(2024, 3, 1)));

        assertEquals(Optional.of(categoryX), guesser.guess("ifood ", history));
    }

    @Test
    void breaksTieByMostRecentTransactionDate() {
        var categoryOld = UUID.randomUUID();
        var categoryRecent = UUID.randomUUID();
        // One each → tie on frequency; categoryRecent has the later date.
        var history = List.of(
                tx("UBER", categoryOld, LocalDate.of(2024, 1, 1)),
                tx("uber", categoryRecent, LocalDate.of(2024, 6, 1)));

        assertEquals(Optional.of(categoryRecent), guesser.guess("Uber", history));
    }

    @Test
    void returnsEmptyWhenNoHistoryMatches() {
        var history = List.of(tx("NETFLIX", UUID.randomUUID(), LocalDate.of(2024, 1, 1)));

        assertTrue(guesser.guess("Spotify", history).isEmpty());
    }
}
