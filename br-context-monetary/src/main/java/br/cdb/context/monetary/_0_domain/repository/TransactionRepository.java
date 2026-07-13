package br.cdb.context.monetary._0_domain.repository;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface TransactionRepository extends Repository<Transaction, UUID> {
    /** Bulk re-key: move every transaction from {@code from} to {@code to} in one statement. */
    void reassignAccount(UUID from, UUID to);

    /** Bulk re-key: move every transaction carrying card {@code from} to card {@code to}. */
    void reassignCard(UUID from, UUID to);
}
