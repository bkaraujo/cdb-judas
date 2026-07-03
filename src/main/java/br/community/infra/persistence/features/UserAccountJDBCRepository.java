package br.community.infra.persistence.features;

import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.core.UserAccount;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) para {@code USER_ACCOUNT} (overlay por utilizador: só a cor).
 * PK composta {@code (COD_USER, COD_ACCOUNT)}.
 */
@NullMarked
public final class UserAccountJDBCRepository extends JDBCRepository<UserAccount> {

    public UserAccountJDBCRepository() {
        super("USER_ACCOUNT");
    }

    public Optional<UserAccount> find(String userId, UUID accountId) {
        return findById(userId, accountId.toString());
    }

    public List<UserAccount> findByUser(String userId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_USER = ?",
                JDBCParameter.of(userId),
                this::mapList
        );
    }

    public void delete(String userId, UUID accountId) {
        deleteById(userId, accountId.toString());
    }

    @Override
    protected Map<String, @Nullable Object> values(UserAccount ua) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_USER", ua.userId());
        values.put("COD_ACCOUNT", ua.accountId().toString());
        values.put("TXT_COLOR", ua.color());
        return values;
    }

    @Override
    protected UserAccount map(JDBCResultSet rs) {
        val userId = rs.getString("COD_USER").get();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val color = rs.getString("TXT_COLOR").get();

        return new UserAccount(userId, accountId, color);
    }
}
