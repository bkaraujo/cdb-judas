package br.cdb.feature.f000._1_application;

import br.cdb.feature.f000._0_domain.ClosingRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
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

    public Result<Void, BusinessError> validateDate(LocalDate date) {
        val closingOpt = closingRepository.find();
        if (closingOpt.isPresent() && !YearMonth.from(date).isAfter(closingOpt.get())) {
            return Result.failure(new BusinessError.BusinessRule("Período fechado. Lançamentos até " + closingOpt.get() + " não podem ser alterados."));
        }
        return Result.success();
    }
}
