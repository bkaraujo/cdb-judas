package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.core.UserAccount;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
                List.of(new JDBCPreparedParameter(1, userId), new JDBCPreparedParameter(2, accountId.toString())),
                this::toUserAccounts
        ).getOrThrow().stream().findFirst();
    }

    public List<UserAccount> findByUser(String userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT WHERE COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, userId)),
                this::toUserAccounts
        ).getOrThrow();
    }

    public void save(UserAccount ua) {
        dataSource.execute(
                "MERGE INTO USER_ACCOUNT (" + COLUMNS + ") KEY(COD_USER, COD_ACCOUNT) VALUES (?, ?, ?, ?, ?)",
                new JDBCPreparedParameter(1, ua.userId()),
                new JDBCPreparedParameter(2, ua.accountId().toString()),
                new JDBCPreparedParameter(3, ua.openingBalance()),
                new JDBCPreparedParameter(4, ua.color()),
                new JDBCPreparedParameter(5, ua.active() ? "Y" : "N")
        ).getOrThrow();
    }

    public void delete(String userId, UUID accountId) {
        dataSource.execute(
                "DELETE FROM USER_ACCOUNT WHERE COD_USER = ? AND COD_ACCOUNT = ?",
                new JDBCPreparedParameter(1, userId),
                new JDBCPreparedParameter(2, accountId.toString())
        ).getOrThrow();
    }

    private List<UserAccount> toUserAccounts(JDBCResultSet rs) {
        val list = new ArrayList<UserAccount>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) list.add(toUserAccount(rs));
        return list;
    }

    private UserAccount toUserAccount(JDBCResultSet rs) {
        val userId = rs.getString("COD_USER").getOrThrow();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").getOrThrow());
        @Nullable BigDecimal bal = rs.getBigDecimal("DEC_OPENING_BALANCE").getOrThrow();
        val openingBalance = bal != null ? bal : BigDecimal.ZERO;
        val color = rs.getString("TXT_COLOR").getOrThrow();
        final boolean active = "Y".equals(rs.getString("FLG_ACTIVE").getOrThrow());
        return new UserAccount(userId, accountId, openingBalance, color, active);
    }
}
