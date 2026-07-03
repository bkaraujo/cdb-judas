package br.community.feature.user.categories;

import br.community.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserCategoryRepository {
    List<UserCategory> findAllByUser(UUID userId);

    List<UserCategory> findByNature(UUID userId, Transaction.Type nature);
    Optional<UserCategory> findById(UUID id);
    UserCategory save(UserCategory category);
    void deleteById(UUID id);
}
