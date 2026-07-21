package br.cdb.feature.f008._2_infrastructure;

import br.cdb.feature.f008._0_domain.UserTransactionTagRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link UserTransactionTagRepository}; tabela {@code PERSON_TRANSACTION_TAG}.
 * Join table pura (PK composta {@code COD_TRANSACTION, COD_PERSON, COD_TAG}), sem entidade de domínio
 * própria — por isso opera direto sobre {@link DataSource} em vez de estender {@code JDBCRepository}.
 */
@NullMarked
public final class UserTransactionTagJDBCRepository implements UserTransactionTagRepository {

    private final DataSource datasource = Registry.get(DataSource.class);

    @Override
    public List<UUID> findTransactionIdsByTag(UUID personId, UUID tagId) {
        return datasource.query(
                "SELECT COD_TRANSACTION FROM PERSON_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ?",
                JDBCParameter.of(personId.toString(), tagId.toString()),
                UserTransactionTagJDBCRepository::readTransactionIds
        );
    }

    @Override
    public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
        // Descarta primeiro o vínculo antigo nas transações que já têm o destino (evita violar a PK).
        datasource.execute(
                "DELETE FROM PERSON_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ? "
                        + "AND COD_TRANSACTION IN (SELECT COD_TRANSACTION FROM PERSON_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ?)",
                JDBCParameter.of(personId.toString(), oldTagId.toString(), personId.toString(), newTagId.toString())
        );
        datasource.execute(
                "UPDATE PERSON_TRANSACTION_TAG SET COD_TAG = ? WHERE COD_TAG = ? AND COD_PERSON = ?",
                JDBCParameter.of(newTagId.toString(), oldTagId.toString(), personId.toString())
        );
    }

    @Override
    public void deleteByTag(UUID personId, UUID tagId) {
        datasource.execute(
                "DELETE FROM PERSON_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ?",
                JDBCParameter.of(personId.toString(), tagId.toString())
        );
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM PERSON_TRANSACTION_TAG WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    private static List<UUID> readTransactionIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_TRANSACTION").get()));
        return ids;
    }
}
