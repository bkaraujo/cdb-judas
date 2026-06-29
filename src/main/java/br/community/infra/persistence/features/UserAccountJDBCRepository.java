package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
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

/**
 * Adaptador JDBC (H2) para {@code USER_ACCOUNT} (overlay por utilizador: saldo, cor, ativo,
 * conta vinculada e dados/limites de cartão).
 */
@NullMarked
public final class UserAccountJDBCRepository {

    private static final String COLUMNS = "COD_USER, COD_ACCOUNT, DEC_OPENING_BALANCE, TXT_COLOR, FLG_ACTIVE,"
            + " COD_LINKED_ACCOUNT, TXT_CARD_LAST4, NUM_DUE_DAY, NUM_CLOSING_DAY, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT";

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
        val active = ua.active() ? "Y" : "N";
        val linked = ua.linkedAccountId() == null ? null : ua.linkedAccountId().toString();
        // Check + write na mesma transação: evita janela de corrida entre SELECT e INSERT/UPDATE
        // na mesma chave (COD_USER, COD_ACCOUNT). A PK composta no banco é o backstop final.
        dataSource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT COD_ACCOUNT FROM USER_ACCOUNT WHERE COD_USER = ? AND COD_ACCOUNT = ?",
                    JDBCParameter.of(ua.userId(), ua.accountId().toString()),
                    rs -> rs.next().get()
            ).get();

            if (exists) {
                tx.execute(
                        "UPDATE USER_ACCOUNT SET DEC_OPENING_BALANCE = ?, TXT_COLOR = ?, FLG_ACTIVE = ?,"
                                + " COD_LINKED_ACCOUNT = ?, TXT_CARD_LAST4 = ?, NUM_DUE_DAY = ?, NUM_CLOSING_DAY = ?,"
                                + " DEC_CREDIT_LIMIT = ?, DEC_OVERDRAFT_LIMIT = ?"
                                + " WHERE COD_USER = ? AND COD_ACCOUNT = ?",
                        JDBCParameter.of(
                                ua.openingBalance(),
                                ua.color(),
                                active,
                                linked,
                                ua.last4(),
                                ua.dueDay(),
                                ua.closingDay(),
                                ua.creditLimit(),
                                ua.overdraftLimit(),
                                ua.userId(),
                                ua.accountId().toString()
                        )
                ).get();
            } else {
                tx.execute(
                        "INSERT INTO USER_ACCOUNT (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        JDBCParameter.of(
                                ua.userId(),
                                ua.accountId().toString(),
                                ua.openingBalance(),
                                ua.color(),
                                active,
                                linked,
                                ua.last4(),
                                ua.dueDay(),
                                ua.closingDay(),
                                ua.creditLimit(),
                                ua.overdraftLimit()
                        )
                ).get();
            }
            return Result.success(true);
        });
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

        final @Nullable String linkedRaw = rs.getString("COD_LINKED_ACCOUNT").get();
        final @Nullable UUID linkedAccountId = (linkedRaw == null || linkedRaw.isBlank()) ? null : UUID.fromString(linkedRaw);

        final @Nullable String last4Raw = rs.getString("TXT_CARD_LAST4").get();
        final @Nullable String last4 = (last4Raw == null || last4Raw.isBlank()) ? null : last4Raw;

        final @Nullable Integer dueDay = rs.getObject("NUM_DUE_DAY", Integer.class).get();
        final @Nullable Integer closingDay = rs.getObject("NUM_CLOSING_DAY", Integer.class).get();
        final @Nullable BigDecimal creditLimit = rs.getBigDecimal("DEC_CREDIT_LIMIT").get();
        final @Nullable BigDecimal overdraftLimit = rs.getBigDecimal("DEC_OVERDRAFT_LIMIT").get();

        return new UserAccount(userId, accountId, openingBalance, color, active,
                linkedAccountId, last4, dueDay, closingDay, creditLimit, overdraftLimit);
    }
}
