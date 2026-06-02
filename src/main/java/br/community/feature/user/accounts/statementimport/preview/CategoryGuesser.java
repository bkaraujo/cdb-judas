package br.community.feature.user.accounts.statementimport.preview;

import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.feature.user.accounts.statementimport.GroupSignature;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Guesses a transaction's category from history: among past transactions whose normalized
 * description matches the target, picks the category used most often, breaking ties by the most
 * recent transaction date. Pure — no injected dependencies.
 */
@NullMarked
public class CategoryGuesser {

    public Optional<UUID> guess(String description, List<MonetaryTransaction> history) {
        final String target = GroupSignature.normalizeDescription(description);

        final List<MonetaryTransaction> matches = history.stream()
                .filter(t -> target.equals(GroupSignature.normalizeDescription(t.description())))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        final Map<UUID, LocalDate> mostRecentByCategory = matches.stream()
                .collect(Collectors.toMap(
                        MonetaryTransaction::categoryId,
                        MonetaryTransaction::date,
                        (a, b) -> a.isAfter(b) ? a : b));

        final Map<UUID, Long> countByCategory = matches.stream()
                .collect(Collectors.groupingBy(MonetaryTransaction::categoryId, Collectors.counting()));

        return countByCategory.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(e -> mostRecentByCategory.get(e.getKey())))
                .map(Map.Entry::getKey);
    }
}
