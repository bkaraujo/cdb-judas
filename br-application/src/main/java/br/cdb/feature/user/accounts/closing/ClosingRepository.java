package br.cdb.feature.user.accounts.closing;

import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Optional;

@NullMarked
public interface ClosingRepository {

    Optional<YearMonth> find();

    void save(YearMonth ym);

    void clear();
}
