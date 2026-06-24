package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

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
        return dataSource.query("SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE", this::toBalances);
    }

    @Override
    public Optional<MonthlyBalance> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toBalances
        ).stream().findFirst();
    }

    @Override
    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_ACCOUNT_BALANCE WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId.toString()),
                this::toBalances
        );
    }

    @Override
    public BigDecimal findOpeningBalance(UUID accountId) {
        val results = dataSource.query(
                "SELECT DEC_OPENING_BALANCE FROM USER_ACCOUNT WHERE COD_ACCOUNT = ? LIMIT 1",
                JDBCParameter.of(accountId.toString()),
                rs -> {
                    val list = new ArrayList<BigDecimal>();
                    while (rs.next().get()) {
                        val b = rs.getBigDecimal("DEC_OPENING_BALANCE").get();
                        list.add(b);
                    }
                    return list;
                }
        );
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
                    JDBCParameter.of(
                            entity.id().toString(),
                            userId,
                            entity.accountId().toString(),
                            numPeriod,
                            entity.balance()
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE USER_ACCOUNT_BALANCE SET COD_USER = ?, COD_ACCOUNT = ?, NUM_PERIOD = ?, DEC_BALANCE = ? WHERE ID = ?",
                    JDBCParameter.of(
                            userId,
                            entity.accountId().toString(),
                            numPeriod,
                            entity.balance(),
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_ACCOUNT_BALANCE WHERE ID = ?", JDBCParameter.of(id.toString()));
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private String findUserIdForAccount(UUID accountId) {
        val results = dataSource.query(
                "SELECT COD_USER FROM USER_ACCOUNT WHERE COD_ACCOUNT = ? LIMIT 1",
                JDBCParameter.of(accountId.toString()),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) {
                        val u = rs.getString("COD_USER").get();
                        list.add(u);
                    }
                    return list;
                }
        );
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
        while (rs.next().get()) balances.add(toBalance(rs));
        return balances;
    }

    private MonthlyBalance toBalance(JDBCResultSet rs) {
        return new MonthlyBalance(
                UUID.fromString(rs.getString("ID").get()),
                UUID.fromString(rs.getString("COD_ACCOUNT").get()),
                fromNumPeriod(rs.getInt("NUM_PERIOD").get()),
                rs.getBigDecimal("DEC_BALANCE").get()
        );
    }
}
