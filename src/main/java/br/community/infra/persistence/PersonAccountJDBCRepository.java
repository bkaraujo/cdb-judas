package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.people._0_domain.repository.PersonAccountRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link PersonAccountRepository}; tabela auxiliar
 * {@code PEP_PERSON_ACCOUNT} (junção pessoa↔conta).
 */
@NullMarked
public final class PersonAccountJDBCRepository implements PersonAccountRepository {

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public void link(UUID personId, UUID accountId) {
        dataSource.execute(
                "MERGE INTO PEP_PERSON_ACCOUNT (COD_PERSON, COD_ACCOUNT) KEY(COD_PERSON, COD_ACCOUNT) VALUES (?, ?)",
                new JDBCPreparedParameter(1, personId.toString()),
                new JDBCPreparedParameter(2, accountId.toString())
        ).getOrThrow();
    }

    @Override
    public void unlink(UUID personId, UUID accountId) {
        dataSource.execute(
                "DELETE FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ? AND COD_ACCOUNT = ?",
                new JDBCPreparedParameter(1, personId.toString()),
                new JDBCPreparedParameter(2, accountId.toString())
        ).getOrThrow();
    }

    @Override
    public List<UUID> accountIdsOf(UUID personId) {
        return dataSource.query(
                "SELECT COD_ACCOUNT FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ?",
                List.of(new JDBCPreparedParameter(1, personId.toString())),
                this::toAccountIds
        ).getOrThrow();
    }

    @Override
    public boolean owns(UUID personId, UUID accountId) {
        return !dataSource.query(
                "SELECT COD_ACCOUNT FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ? AND COD_ACCOUNT = ?",
                List.of(
                        new JDBCPreparedParameter(1, personId.toString()),
                        new JDBCPreparedParameter(2, accountId.toString())
                ),
                this::toAccountIds
        ).getOrThrow().isEmpty();
    }

    private List<UUID> toAccountIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) ids.add(UUID.fromString(rs.getString("COD_ACCOUNT").getOrThrow()));
        return ids;
    }
}
