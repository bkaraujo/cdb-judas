package br.cdb.feature.f006._1_application.service;

import br.cdb.feature.f006._0_domain.repository.TransactionTagRepository;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Context-wired, sem CDI ({@code Context.tryGet(TransactionTagService.class)}) — usado direto por
 *  {@code WriteUseCases}/{@code ReadUseCases} e por {@code F006Module} (reação a {@code TagEvents.Deleted}). */
@NullMarked
public class TransactionTagService {

    private final TransactionTagRepository repository = Context.get(TransactionTagRepository.class);

    public void replaceTags(UUID transactionId, UUID personId, List<UUID> tagIds) {
        repository.replaceTags(transactionId, personId, tagIds);
    }

    public Map<UUID, List<UUID>> findTagsByPerson(UUID personId) {
        return repository.findTagsByPerson(personId);
    }

    public void deleteTagsByTransaction(UUID transactionId) {
        repository.deleteTagsByTransaction(transactionId);
    }

    public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
        repository.reassignTag(oldTagId, newTagId, personId);
    }

    public void detachTag(UUID tagId, UUID personId) {
        repository.detachTag(tagId, personId);
    }
}
