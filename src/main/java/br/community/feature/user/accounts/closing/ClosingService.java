package br.community.feature.user.accounts.closing;

import br.commons.Result;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@NullMarked
@RequiredArgsConstructor
public class ClosingService {

    private final ClosingRepository closingRepository;

    public Optional<YearMonth> find() {
        return closingRepository.find();
    }

    public YearMonth save(YearMonth ym) {
        closingRepository.save(ym);
        return ym;
    }

    public void clear() {
        closingRepository.clear();
    }

    public Result<Void, DomainError> validateDate(LocalDate date) {
        val closingOpt = closingRepository.find();
        if (closingOpt.isPresent() && !YearMonth.from(date).isAfter(closingOpt.get())) {
            return Result.failure(new DomainError.BusinessRule(
                    "Período fechado. Lançamentos até " + closingOpt.get() + " não podem ser alterados."));
        }
        return Result.success();
    }
}
