package br.cdb.feature.f006._1_application;

import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Deterministic identity for imported charges so re-imports recognize the same purchase.
 *
 * <p>The parcelado group personId hashes {@code accountId}, the ORIGINAL purchase date, the installment
 * total and the normalized description — deliberately EXCLUDING the amount: Santander spreads
 * rounding cents across installments (e.g. 231,81 one month, 231,79 the next), so the amount is not
 * stable across statements. The à-vista key composes the same fields plus the amount, since a single
 * charge has no installment schedule to anchor it.
 */
@NullMarked
public class GroupSignature {

    public static String normalize(String description) {
        return Strings.upper(description.trim().replaceAll("\\s+", " "));
    }

    public UUID groupId(UUID accountId, LocalDate originalDate, int totalInstallments, String description) {
        val canonical = accountId + "|" + originalDate + "|" + totalInstallments + "|" + normalize(description);
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public String avistaKey(UUID accountId, LocalDate date, BigDecimal amount, String description) {
        return accountId + "|" + date + "|" + amount.stripTrailingZeros().toPlainString() + "|" + normalize(description);
    }
}
