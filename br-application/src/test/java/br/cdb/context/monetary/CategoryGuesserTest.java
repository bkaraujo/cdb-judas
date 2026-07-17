package br.cdb.context.monetary;

import br.cdb.feature.finance.accounts.transactions.importer.CategoryGuesser;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class CategoryGuesserTest {

    private final CategoryGuesser guesser = new CategoryGuesser();

    private static CategoryGuesser.Entry entry(String description, UUID categoryId, LocalDate date) {
        return new CategoryGuesser.Entry(description, categoryId, date);
    }

    @Test
    void returnsMostFrequentCategoryAcrossNormalizedDescriptions() {
        var categoryX = UUID.randomUUID();
        var categoryY = UUID.randomUUID();
        var history = List.of(
                entry("IFOOD", categoryX, LocalDate.of(2024, 1, 1)),
                entry("ifood ", categoryX, LocalDate.of(2024, 2, 1)),
                entry(" iFood", categoryY, LocalDate.of(2024, 3, 1)));

        assertEquals(Optional.of(categoryX), guesser.guess("ifood ", history));
    }

    @Test
    void breaksTieByMostRecentTransactionDate() {
        var categoryOld = UUID.randomUUID();
        var categoryRecent = UUID.randomUUID();
        var history = List.of(
                entry("UBER", categoryOld, LocalDate.of(2024, 1, 1)),
                entry("uber", categoryRecent, LocalDate.of(2024, 6, 1)));

        assertEquals(Optional.of(categoryRecent), guesser.guess("Uber", history));
    }

    @Test
    void returnsEmptyWhenNoHistoryMatches() {
        var history = List.of(entry("NETFLIX", UUID.randomUUID(), LocalDate.of(2024, 1, 1)));

        assertTrue(guesser.guess("Spotify", history).isEmpty());
    }
}
