package br.cdb.feature.f006._1_application;

import br.cdb.feature.f006._0_domain.UserTransaction;
import br.cdb.feature.f006._0_domain.UserTransactionRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço do vínculo transação↔categoria ({@code F005_TRANSACTION_CATEGORY}). Conta e pessoa da
 * transação já são colunas nativas de {@code F006_TRANSACTION} desde a fusão dos contextos — este
 * serviço só cuida da categoria. {@code accountId} continua no parâmetro de {@link #save} por
 * compatibilidade com a porta {@code TransactionOverlaySink} (f007); não é mais persistido aqui.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTransactionService {

    private final UserTransactionRepository repo;

    public UserTransaction save(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId) {
        return repo.save(new UserTransaction(transactionId, personId, categoryId, null, null));
    }

    public Optional<UserTransaction> find(UUID transactionId, UUID personId) {
        return repo.findByTransactionAndPerson(transactionId, personId);
    }

    public List<UserTransaction> findAllByPerson(UUID personId) {
        return repo.findAllByPerson(personId);
    }

    public Map<UUID, UserTransaction> indexByTransaction(UUID personId) {
        return findAllByPerson(personId).stream()
                .collect(Collectors.toMap(UserTransaction::transactionId, ut -> ut));
    }

    public void deleteByTransaction(UUID transactionId) {
        repo.deleteByTransaction(transactionId);
    }

    public void deleteByTransactionAndPerson(UUID transactionId, UUID personId) {
        repo.deleteByTransactionAndPerson(transactionId, personId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        repo.reassignCategory(oldCategoryId, newCategoryId, personId);
    }

    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return repo.findTransactionIdsByCategories(personId, categoryIds);
    }
}
