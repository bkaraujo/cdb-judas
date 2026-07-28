package br.cdb.feature.f004._1_application;

import br.cdb.feature.f004._0_domain.UserTransactionTagRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTransactionTagService {

    private final UserTransactionTagRepository repo;

    public List<UUID> findTransactionIdsByTag(UUID personId, UUID tagId) {
        return repo.findTransactionIdsByTag(personId, tagId);
    }

    public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
        repo.reassignTag(oldTagId, newTagId, personId);
    }

    public void deleteByTag(UUID personId, UUID tagId) {
        repo.deleteByTag(personId, tagId);
    }

    public void deleteByTransaction(UUID transactionId) {
        repo.deleteByTransaction(transactionId);
    }
}
