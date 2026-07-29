package br.cdb.feature.f002._1_application;

import br.cdb.feature.f002._0_domain.ClosingRepository;
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

    private final ClosingRepository repository;

    public Optional<YearMonth> find() {
        return repository.find();
    }

    public YearMonth save(YearMonth ym) {
        repository.save(ym);
        return ym;
    }

    public void clear() {
        repository.clear();
    }

    public Result<Void, BusinessError> validateDate(LocalDate date) {
        val optional = repository.find();
        if (optional.isPresent() && !YearMonth.from(date).isAfter(optional.get())) {
            return Result.failure(new BusinessError.BusinessRule("Período fechado. Lançamentos até " + optional.get() + " não podem ser alterados."));
        }
        return Result.success();
    }
}
