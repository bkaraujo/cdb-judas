package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.tags.UserTransactionTagRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link UserTransactionTagRepository}; tabela {@code USER_TRANSACTION_TAG}.
 * Join table pura (PK composta {@code COD_TRANSACTION, COD_USER, COD_TAG}), sem entidade de domínio
 * própria — por isso opera direto sobre {@link DataSource} em vez de estender {@code JDBCRepository}.
 */
@NullMarked
public final class UserTransactionTagJDBCRepository implements UserTransactionTagRepository {

    private final DataSource datasource = Registry.get(DataSource.class);

    @Override
    public List<UUID> findTransactionIdsByTag(UUID userId, UUID tagId) {
        return datasource.query(
                "SELECT COD_TRANSACTION FROM USER_TRANSACTION_TAG WHERE COD_USER = ? AND COD_TAG = ?",
                JDBCParameter.of(userId.toString(), tagId.toString()),
                UserTransactionTagJDBCRepository::readTransactionIds
        );
    }

    @Override
    public void reassignTag(UUID oldTagId, UUID newTagId, UUID userId) {
        // Descarta primeiro o vínculo antigo nas transações que já têm o destino (evita violar a PK).
        datasource.execute(
                "DELETE FROM USER_TRANSACTION_TAG WHERE COD_USER = ? AND COD_TAG = ? "
                        + "AND COD_TRANSACTION IN (SELECT COD_TRANSACTION FROM USER_TRANSACTION_TAG WHERE COD_USER = ? AND COD_TAG = ?)",
                JDBCParameter.of(userId.toString(), oldTagId.toString(), userId.toString(), newTagId.toString())
        );
        datasource.execute(
                "UPDATE USER_TRANSACTION_TAG SET COD_TAG = ? WHERE COD_TAG = ? AND COD_USER = ?",
                JDBCParameter.of(newTagId.toString(), oldTagId.toString(), userId.toString())
        );
    }

    @Override
    public void deleteByTag(UUID userId, UUID tagId) {
        datasource.execute(
                "DELETE FROM USER_TRANSACTION_TAG WHERE COD_USER = ? AND COD_TAG = ?",
                JDBCParameter.of(userId.toString(), tagId.toString())
        );
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM USER_TRANSACTION_TAG WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    private static List<UUID> readTransactionIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_TRANSACTION").get()));
        return ids;
    }
}
