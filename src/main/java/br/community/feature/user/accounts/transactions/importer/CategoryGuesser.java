package br.community.feature.user.accounts.transactions.importer;

import br.community.context.monetary._0_domain.model.Transaction;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Guesses a transaction's category from history: among past transactions whose normalized
 * description matches the target, picks the category used most often, breaking ties by the most
 * recent transaction date. Pure — no injected dependencies.
 */
@NullMarked
public class CategoryGuesser {

    public Optional<UUID> guess(String description, List<Transaction> history) {
        val target = GroupSignature.normalize(description);

        val matches = history.stream()
                .filter(t -> target.equals(GroupSignature.normalize(t.description())))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        val mostRecentByCategory = matches.stream()
                .collect(Collectors.toMap(
                        Transaction::categoryId,
                        Transaction::date,
                        (a, b) -> a.isAfter(b) ? a : b));

        val countByCategory = matches.stream()
                .collect(Collectors.groupingBy(Transaction::categoryId, Collectors.counting()));

        return countByCategory.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(e -> mostRecentByCategory.get(e.getKey())))
                .map(Map.Entry::getKey);
    }
}
