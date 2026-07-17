package br.cdb.feature.finance.accounts.transactions.importer;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Guesses a transaction's category from history: among past entries whose normalized description
 * matches the target, picks the category used most often, breaking ties by the most recent date.
 * Pure — no injected dependencies.
 */
@NullMarked
public class CategoryGuesser {

    @NullMarked
    public record Entry(String description, UUID categoryId, LocalDate date) {}

    public Optional<UUID> guess(String description, List<Entry> history) {
        val target = GroupSignature.normalize(description);

        val matches = history.stream()
                .filter(e -> target.equals(GroupSignature.normalize(e.description())))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        val mostRecentByCategory = matches.stream()
                .collect(Collectors.toMap(
                        Entry::categoryId,
                        Entry::date,
                        (a, b) -> a.isAfter(b) ? a : b));

        val countByCategory = matches.stream()
                .collect(Collectors.groupingBy(Entry::categoryId, Collectors.counting()));

        return countByCategory.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(e -> mostRecentByCategory.get(e.getKey())))
                .map(Map.Entry::getKey);
    }
}
