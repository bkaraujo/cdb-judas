package br.cdb.feature.f002._0_domain;

import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Optional;

@NullMarked
public interface ClosingRepository {

    Optional<YearMonth> find();

    void save(YearMonth ym);

    void clear();
}
