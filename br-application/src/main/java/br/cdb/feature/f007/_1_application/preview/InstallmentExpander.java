package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f007._0_domain.model.MonetaryDocumentEntry;
import br.cdb.feature.f007._0_domain.model.TransactionDraft;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Expands a year-less statement line into absolute-dated drafts.
 *
 * <p>Banks print day/month only, so the original absolute date is inferred from the statement period
 * (the invoice's own printed month/year, parsed by the caller) and the installment offset: installment
 * {@code n} bills ≈ the statement month,
 * so the original purchase ≈ {@code statementMonth − (n−1)}. That anchor picks the YEAR for the
 * printed month/day. À-vista lines produce one draft; parcelado {@code n/N} lines produce {@code N}
 * drafts (the full schedule), all sharing the same {@code groupId}. Status is by date: this month or
 * earlier → {@code "confirmed"}, future month → {@code "scheduled"}.
 */
@NullMarked
@RequiredArgsConstructor
public class InstallmentExpander {

    private final GroupSignature groupSignature;

    public List<TransactionDraft> expand(MonetaryDocumentEntry line, UUID accountId, YearMonth statementPeriod, LocalDate today) {
        val n = line.isInstallment() ? line.installmentNumber() : 1;
        val anchor = line.isInstallment() ? statementPeriod.minusMonths(n - 1L) : statementPeriod;
        val originalDate = resolveOriginalDate(MonthDay.from(line.date()), anchor);

        if (!line.isInstallment()) {
            return List.of(new TransactionDraft(
                    line.last4(), originalDate, originalDate, line.description(), line.amount(),
                    statusFor(originalDate, today), line.type(), accountId, null, null, null, line.kind()));
        }

        val total = line.installmentTotal();
        val groupId = groupSignature.groupId(accountId, originalDate, total, line.description());
        val drafts = new ArrayList<TransactionDraft>(total);
        for (int k = 1; k <= total; k++) {
            val date = originalDate.plusMonths(k - 1L);
            drafts.add(new TransactionDraft(
                    line.last4(), date, originalDate, line.description(), line.amount(),
                    statusFor(date, today), line.type(), accountId, groupId, k, total, line.kind()));
        }
        return List.copyOf(drafts);
    }

    private static LocalDate resolveOriginalDate(MonthDay monthDay, YearMonth anchor) {
        int year = anchor.getYear();
        val diff = monthDay.getMonthValue() - anchor.getMonthValue();
        if (diff > 6) {
            year--;
        } else if (diff < -6) {
            year++;
        }
        return monthDay.atYear(year);
    }

    private static Status statusFor(LocalDate d, LocalDate today) {
        return YearMonth.from(d).isAfter(YearMonth.from(today)) ? Status.SCHEDULED : Status.CONFIRMED;
    }
}
