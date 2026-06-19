package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link BalanceRepository}; tabela {@code MON_MONTHLY_BALANCE}. */
@NullMarked
public final class MonthlyBalanceJDBCRepository implements BalanceRepository {

    private static final String COLUMNS = "ID, COD_ACCOUNT, TXT_PERIOD, DEC_BALANCE";

    private final DataSource dataSource;

    public MonthlyBalanceJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<MonthlyBalance> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM MON_MONTHLY_BALANCE", this::toBalances).getOrThrow();
    }

    @Override
    public Optional<MonthlyBalance> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_MONTHLY_BALANCE WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toBalances
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_MONTHLY_BALANCE WHERE COD_ACCOUNT = ?",
                List.of(new JDBCPreparedParameter(1, accountId.toString())),
                this::toBalances
        ).getOrThrow();
    }

    @Override
    public MonthlyBalance save(MonthlyBalance entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_MONTHLY_BALANCE (" + COLUMNS + ") VALUES (?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.accountId().toString()),
                    new JDBCPreparedParameter(3, entity.period().toString()),
                    new JDBCPreparedParameter(4, entity.balance())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_MONTHLY_BALANCE SET COD_ACCOUNT = ?, TXT_PERIOD = ?, DEC_BALANCE = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.accountId().toString()),
                    new JDBCPreparedParameter(2, entity.period().toString()),
                    new JDBCPreparedParameter(3, entity.balance()),
                    new JDBCPreparedParameter(4, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_MONTHLY_BALANCE WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
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
                YearMonth.parse(rs.getString("TXT_PERIOD").getOrThrow()),
                rs.getBigDecimal("DEC_BALANCE").getOrThrow()
        );
    }
}
