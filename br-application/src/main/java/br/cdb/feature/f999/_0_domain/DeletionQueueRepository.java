package br.cdb.feature.f999._0_domain;

import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public interface DeletionQueueRepository extends Repository<DeletionQueueEntry, UUID> {

    /** Linhas não travadas (candidatas a retry), mais antigas primeiro. */
    List<DeletionQueueEntry> findAllUnlocked();

}
