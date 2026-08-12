package br.cdb.feature.f005._0_domain;

import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface CategoryRepository {
    List<Category> findAllByPerson(UUID personId);

    List<Category> findByNature(UUID personId, Transaction.Type nature);
    Optional<Category> findById(UUID id);
    Category save(Category category);
    void deleteById(UUID id);
}
