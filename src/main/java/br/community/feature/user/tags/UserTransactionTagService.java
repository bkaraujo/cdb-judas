package br.community.feature.user.tags;

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

    public List<UUID> findTransactionIdsByTag(UUID userId, UUID tagId) {
        return repo.findTransactionIdsByTag(userId, tagId);
    }

    public void reassignTag(UUID oldTagId, UUID newTagId, UUID userId) {
        repo.reassignTag(oldTagId, newTagId, userId);
    }

    public void deleteByTag(UUID userId, UUID tagId) {
        repo.deleteByTag(userId, tagId);
    }

    public void deleteByTransaction(UUID transactionId) {
        repo.deleteByTransaction(transactionId);
    }
}
