package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.core.UserAccount;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) para {@code USER_ACCOUNT} (overlay por utilizador: saldo, cor, ativo). */
@NullMarked
public final class UserAccountJDBCRepository {

    private static final String COLUMNS = "COD_USER, COD_ACCOUNT, DEC_OPENING_BALANCE, TXT_COLOR, FLG_ACTIVE";

    private final DataSource dataSource = Registry.get(DataSource.class);

    public Optional<UserAccount> find(String userId, UUID accountId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT WHERE COD_USER = ? AND COD_ACCOUNT = ?",
                JDBCParameter.of(userId, accountId.toString()),
                this::toUserAccounts
        ).stream().findFirst();
    }

    public List<UserAccount> findByUser(String userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT WHERE COD_USER = ?",
                JDBCParameter.of(userId),
                this::toUserAccounts
        );
    }

    public void save(UserAccount ua) {
        dataSource.execute(
                "MERGE INTO USER_ACCOUNT (" + COLUMNS + ") KEY(COD_USER, COD_ACCOUNT) VALUES (?, ?, ?, ?, ?)",
                JDBCParameter.of(
                        ua.userId(),
                        ua.accountId().toString(),
                        ua.openingBalance(),
                        ua.color(),
                        ua.active() ? "Y" : "N"
                )
        );
    }

    public void delete(String userId, UUID accountId) {
        dataSource.execute(
                "DELETE FROM USER_ACCOUNT WHERE COD_USER = ? AND COD_ACCOUNT = ?",
                JDBCParameter.of(userId, accountId.toString())
        );
    }

    private List<UserAccount> toUserAccounts(JDBCResultSet rs) {
        val list = new ArrayList<UserAccount>();
        while (rs.next().get()) list.add(toUserAccount(rs));
        return list;
    }

    private UserAccount toUserAccount(JDBCResultSet rs) {
        val userId = rs.getString("COD_USER").get();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val bal = rs.getBigDecimal("DEC_OPENING_BALANCE").get();
        val openingBalance = bal != null ? bal : BigDecimal.ZERO;
        val color = rs.getString("TXT_COLOR").get();
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());
        return new UserAccount(userId, accountId, openingBalance, color, active);
    }
}
