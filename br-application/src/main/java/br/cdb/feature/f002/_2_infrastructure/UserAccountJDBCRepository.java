package br.cdb.feature.f002._2_infrastructure;

import br.cdb.feature.f002._0_domain.UserAccount;
import br.cdb.feature.f002._0_domain.UserAccountRepository;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link UserAccountRepository}; tabela {@code PERSON_ACCOUNT}
 * (overlay por utilizador: só a cor). PK composta {@code (COD_PERSON, COD_ACCOUNT)}.
 */
@NullMarked
public final class UserAccountJDBCRepository extends JDBCRepository<UserAccount> implements UserAccountRepository {

    public UserAccountJDBCRepository() {
        super("PERSON_ACCOUNT");
    }

    @Override
    public Optional<UserAccount> find(String personId, UUID accountId) {
        return findById(personId, accountId.toString());
    }

    @Override
    public List<UserAccount> findByPerson(String personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId),
                this::mapList
        );
    }

    @Override
    public void delete(String personId, UUID accountId) {
        deleteById(personId, accountId.toString());
    }

    @Override
    protected Map<String, @Nullable Object> values(UserAccount ua) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_PERSON", ua.personId());
        values.put("COD_ACCOUNT", ua.accountId().toString());
        values.put("TXT_COLOR", ua.color());
        return values;
    }

    @Override
    protected UserAccount map(JDBCResultSet rs) {
        val personId = rs.getString("COD_PERSON").get();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val color = rs.getString("TXT_COLOR").get();

        return new UserAccount(personId, accountId, color);
    }
}
