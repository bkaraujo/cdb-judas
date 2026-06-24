package br.community.infra.persistence.person;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
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
                JDBCParameter.of (
                        personId.toString(),
                        accountId.toString()
                )
        );
    }

    @Override
    public void unlink(UUID personId, UUID accountId) {
        dataSource.execute(
                "DELETE FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ? AND COD_ACCOUNT = ?",
                JDBCParameter.of (
                        personId.toString(),
                        accountId.toString()
                )
        );
    }

    @Override
    public List<UUID> accountIdsOf(UUID personId) {
        return dataSource.query(
                "SELECT COD_ACCOUNT FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                this::toAccountIds
        );
    }

    @Override
    public boolean owns(UUID personId, UUID accountId) {
        return !dataSource.query(
                "SELECT COD_ACCOUNT FROM PEP_PERSON_ACCOUNT WHERE COD_PERSON = ? AND COD_ACCOUNT = ?",
                JDBCParameter.of (
                        personId.toString(),
                        accountId.toString()
                ),
                this::toAccountIds
        ).isEmpty();
    }

    private List<UUID> toAccountIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_ACCOUNT").get()));
        return ids;
    }
}
