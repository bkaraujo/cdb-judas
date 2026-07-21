package br.cdb.feature.f007._0_domain;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserCategoryRepository {
    List<UserCategory> findAllByPerson(UUID personId);

    List<UserCategory> findByNature(UUID personId, Transaction.Type nature);
    Optional<UserCategory> findById(UUID id);
    UserCategory save(UserCategory category);
    void deleteById(UUID id);
}
