package br.cdb.feature.f002._1_application.service;

import br.cdb.feature.f002._0_domain.repository.ClosingRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

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

}
