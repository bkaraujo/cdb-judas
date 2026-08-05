package br.cdb.feature.f006._2_infrastructure.persistence;

import br.cdb.feature.f006._0_domain.repository.TransactionTagRepository;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link TransactionTagRepository}; tabela {@code F006_TRANSACTION_TAG}.
 * Join table pura (PK composta {@code COD_TRANSACTION, COD_PERSON, COD_TAG}), sem entidade de domínio
 * própria — por isso opera direto sobre {@link DataSource} em vez de estender {@code JDBCRepository}
 * (mesmo molde de {@link TransactionCategoryJDBCRepository}).
 */
@NullMarked
public final class TransactionTagJDBCRepository implements TransactionTagRepository {

    private final DataSource datasource = Context.get(DataSource.class);

    /** DELETE + INSERT (a tabela só tem as três colunas da chave, nada a preservar num UPDATE). */
    @Override
    public void replaceTags(UUID transactionId, UUID personId, List<UUID> tagIds) {
        datasource.transaction(tx -> {
            tx.execute(
                    "DELETE FROM F006_TRANSACTION_TAG WHERE COD_TRANSACTION = ? AND COD_PERSON = ?",
                    JDBCParameter.of(transactionId.toString(), personId.toString())
            ).get();

            for (val tagId : tagIds) {
                tx.execute(
                        "INSERT INTO F006_TRANSACTION_TAG (COD_TRANSACTION, COD_PERSON, COD_TAG) VALUES (?, ?, ?)",
                        JDBCParameter.of(transactionId.toString(), personId.toString(), tagId.toString())
                ).get();
            }

            return Result.success(true);
        });
    }

    @Override
    public Map<UUID, List<UUID>> findTagsByPerson(UUID personId) {
        return datasource.query(
                "SELECT COD_TRANSACTION, COD_TAG FROM F006_TRANSACTION_TAG WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                TransactionTagJDBCRepository::readTagsByTransaction
        );
    }

    @Override
    public void deleteTagsByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM F006_TRANSACTION_TAG WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    @Override
    public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
        // Descarta primeiro o vínculo antigo nas transações que já têm o destino (evita violar a PK composta).
        datasource.execute(
                "DELETE FROM F006_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ? "
                        + "AND COD_TRANSACTION IN (SELECT COD_TRANSACTION FROM F006_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ?)",
                JDBCParameter.of(personId.toString(), oldTagId.toString(), personId.toString(), newTagId.toString())
        );
        datasource.execute(
                "UPDATE F006_TRANSACTION_TAG SET COD_TAG = ? WHERE COD_TAG = ? AND COD_PERSON = ?",
                JDBCParameter.of(newTagId.toString(), oldTagId.toString(), personId.toString())
        );
    }

    @Override
    public void detachTag(UUID tagId, UUID personId) {
        datasource.execute(
                "DELETE FROM F006_TRANSACTION_TAG WHERE COD_PERSON = ? AND COD_TAG = ?",
                JDBCParameter.of(personId.toString(), tagId.toString())
        );
    }

    private static Map<UUID, List<UUID>> readTagsByTransaction(JDBCResultSet rs) {
        val byTransaction = new LinkedHashMap<UUID, List<UUID>>();
        while (rs.next().get()) {
            val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").get());
            val tagId = UUID.fromString(rs.getString("COD_TAG").get());
            byTransaction.computeIfAbsent(transactionId, ignored -> new ArrayList<>()).add(tagId);
        }
        return byTransaction;
    }
}
