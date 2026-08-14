package br.cdb.feature.f002._2_infrastructure.persistence;

import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link BalanceRepository}: tabela {@code F002_BALANCE}
 * (períodos mensais por utilizador, período como YYYYMM inteiro). {@code FLG_DIRTY} é sempre
 * gravado 'N' em {@link #save}/{@link #values} — todo recompute passa por lá e já corrige o valor,
 * então a linha nunca "fica" suja depois de recalculada; {@link #markDirty} é a única escrita que
 * grava 'Y', consumida pela cascata de exclusão de conta (f002) e varrida pelo job de reconciliação
 * (fila {@code F999_DELETION_QUEUE}, f999).
 */
@NullMarked
public final class UserAccountBalanceJDBCRepository extends JDBCRepository<Balance> implements BalanceRepository {

    public UserAccountBalanceJDBCRepository() {
        super("F002_BALANCE");
    }

    @Override
    public Optional<Balance> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public List<Balance> findByAccount(UUID accountId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId.toString()),
                this::mapList
        );
    }

    /**
     * Upsert por (COD_ACCOUNT, NUM_PERIOD): {@link Balance} não carrega {@code personId} — a PK
     * física ({@code ID}) é gerada aqui só no INSERT; a existência é checada pela chave de negócio.
     */
    @Override
    public Balance save(Balance entity) {
        val accountId = entity.account().id().toString();
        val numPeriod = toNumPeriod(entity.period());
        val codPerson = findPersonIdForAccount(entity.account().id());

        datasource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT 1 FROM " + table() + " WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                    JDBCParameter.of(accountId, numPeriod),
                    rs -> rs.next().get()
            ).get();

            val now = Timestamp.valueOf(Time.now());
            if (exists) {
                tx.execute(
                        "UPDATE " + table() + " SET DEC_BALANCE = ?, FLG_DIRTY = 'N', TMS_UPDATED_AT = ? WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                        JDBCParameter.of(entity.value(), now, accountId, numPeriod)
                ).get();
            } else {
                tx.execute(
                        "INSERT INTO " + table() + " (ID, COD_PERSON, COD_ACCOUNT, NUM_PERIOD, DEC_BALANCE, FLG_DIRTY, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?, 'N', ?, ?)",
                        JDBCParameter.of(UUID.randomUUID().toString(), codPerson, accountId, numPeriod, entity.value(), now, now)
                ).get();
            }

            return Result.success(true);
        });

        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    /** Exclui pelo par de negócio (conta, período) — é o que {@code BalanceService.deleteById} usa hoje. */
    @Override
    public void delete(UUID uuid, YearMonth period) {
        datasource.execute(
                "DELETE FROM " + table() + " WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                JDBCParameter.of(uuid.toString(), toNumPeriod(period))
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    @Override
    public void markDirty(UUID accountId) {
        datasource.execute(
                "UPDATE " + table() + " SET FLG_DIRTY = 'Y' WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId.toString())
        );
    }

    @Override
    public void markAllDirty() {
        datasource.execute("UPDATE " + table() + " SET FLG_DIRTY = 'Y'");
    }

    @Override
    public List<UUID> findDirtyAccountIds() {
        return datasource.query(
                "SELECT DISTINCT COD_ACCOUNT FROM " + table() + " WHERE FLG_DIRTY = 'Y'",
                rs -> {
                    val ids = new ArrayList<UUID>();
                    while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_ACCOUNT").get()));
                    return ids;
                }
        );
    }

    @Override
    protected Map<String, @Nullable Object> values(Balance entity) {
        val now = Timestamp.valueOf(Time.now());
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_PERSON", findPersonIdForAccount(entity.account().id()));
        values.put("COD_ACCOUNT", entity.account().id().toString());
        values.put("NUM_PERIOD", toNumPeriod(entity.period()));
        values.put("DEC_BALANCE", entity.value());
        values.put("FLG_DIRTY", "N");
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    /** Resolve o {@code Account} pela porta no Context (lazy — a ordem de registro dos adaptadores não importa); a FK de COD_ACCOUNT garante que a conta existe. */
    @Override
    protected Balance map(JDBCResultSet rs) {
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val account = Context.get(AccountRepository.class).findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account not found for balance row: " + accountId));
        return new Balance(
                account,
                fromNumPeriod(rs.getInt("NUM_PERIOD").get()),
                rs.getBigDecimal("DEC_BALANCE").get()
        );
    }

    private String findPersonIdForAccount(UUID accountId) {
        val results = datasource.query(
                "SELECT COD_PERSON FROM F002_ACCOUNT WHERE ID = ? LIMIT 1",
                JDBCParameter.of(accountId.toString()),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) list.add(rs.getString("COD_PERSON").get());
                    return list;
                }
        );
        return results.isEmpty() ? "" : results.getFirst();
    }

    private static int toNumPeriod(YearMonth period) {
        return period.getYear() * 100 + period.getMonthValue();
    }

    private static YearMonth fromNumPeriod(int numPeriod) {
        return YearMonth.of(numPeriod / 100, numPeriod % 100);
    }
}
