package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link BalanceRepository}: tabela {@code USER_ACCOUNT_BALANCE}
 * (períodos mensais por utilizador, período como YYYYMM inteiro). O saldo de abertura é lido
 * de {@code USER_ACCOUNT.DEC_OPENING_BALANCE}.
 */
@NullMarked
public final class UserAccountBalanceJDBCRepository implements BalanceRepository {

    private static final String COLUMNS = "ID, COD_USER, COD_ACCOUNT, NUM_PERIOD, DEC_BALANCE";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<MonthlyBalance> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE", this::toBalances).getOrThrow();
    }

    @Override
    public Optional<MonthlyBalance> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toBalances
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE WHERE COD_ACCOUNT = ?",
                List.of(new JDBCPreparedParameter(1, accountId.toString())),
                this::toBalances
        ).getOrThrow();
    }

    @Override
    public BigDecimal findOpeningBalance(UUID accountId) {
        val results = dataSource.query(
                "SELECT DEC_OPENING_BALANCE FROM USER_ACCOUNT WHERE COD_ACCOUNT = ? LIMIT 1",
                List.of(new JDBCPreparedParameter(1, accountId.toString())),
                rs -> {
                    val list = new ArrayList<BigDecimal>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable BigDecimal b = rs.getBigDecimal("DEC_OPENING_BALANCE").getOrThrow();
                        if (b != null) list.add(b);
                    }
                    return list;
                }
        ).getOrThrow();
        return results.isEmpty() ? BigDecimal.ZERO : results.get(0);
    }

    @Override
    public MonthlyBalance save(MonthlyBalance entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val numPeriod = toNumPeriod(entity.period());
        val userId = findUserIdForAccount(entity.accountId());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_ACCOUNT_BALANCE (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, userId),
                    new JDBCPreparedParameter(3, entity.accountId().toString()),
                    new JDBCPreparedParameter(4, numPeriod),
                    new JDBCPreparedParameter(5, entity.balance())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE USER_ACCOUNT_BALANCE SET COD_USER = ?, COD_ACCOUNT = ?, NUM_PERIOD = ?, DEC_BALANCE = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, userId),
                    new JDBCPreparedParameter(2, entity.accountId().toString()),
                    new JDBCPreparedParameter(3, numPeriod),
                    new JDBCPreparedParameter(4, entity.balance()),
                    new JDBCPreparedParameter(5, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_ACCOUNT_BALANCE WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private String findUserIdForAccount(UUID accountId) {
        val results = dataSource.query(
                "SELECT COD_USER FROM USER_ACCOUNT WHERE COD_ACCOUNT = ? LIMIT 1",
                List.of(new JDBCPreparedParameter(1, accountId.toString())),
                rs -> {
                    val list = new ArrayList<String>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable String u = rs.getString("COD_USER").getOrThrow();
                        if (u != null) list.add(u);
                    }
                    return list;
                }
        ).getOrThrow();
        return results.isEmpty() ? "" : results.get(0);
    }

    private static int toNumPeriod(YearMonth period) {
        return period.getYear() * 100 + period.getMonthValue();
    }

    private static YearMonth fromNumPeriod(int numPeriod) {
        return YearMonth.of(numPeriod / 100, numPeriod % 100);
    }

    private List<MonthlyBalance> toBalances(JDBCResultSet rs) {
        val balances = new ArrayList<MonthlyBalance>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) balances.add(toBalance(rs));
        return balances;
    }

    private MonthlyBalance toBalance(JDBCResultSet rs) {
        return new MonthlyBalance(
                UUID.fromString(rs.getString("ID").getOrThrow()),
                UUID.fromString(rs.getString("COD_ACCOUNT").getOrThrow()),
                fromNumPeriod(rs.getInt("NUM_PERIOD").getOrThrow()),
                rs.getBigDecimal("DEC_BALANCE").getOrThrow()
        );
    }
}
