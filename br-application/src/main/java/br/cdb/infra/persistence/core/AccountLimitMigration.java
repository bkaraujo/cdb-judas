package br.cdb.infra.persistence.core;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Migração one-shot do DB de dev file-based: limite de crédito/cheque especial e ciclo de fatura
 * deixaram de ser {@code MON_ACCOUNT_LIMIT} (linha própria por conta) e viraram colunas direto em
 * {@code MON_ACCOUNT}.
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code ContextBridge.dataSource(...)} logo após
 * {@link LegacyCardMigration#apply(DataSource)}. Detecta se a tabela {@code MON_ACCOUNT_LIMIT}
 * ainda existe; se não — banco novo ou já migrado — não faz nada. Roda um backup online (arquivo
 * {@code .zip}) antes de qualquer alteração.
 */
@NullMarked
public final class AccountLimitMigration {

    private AccountLimitMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando limites de conta (MON_ACCOUNT_LIMIT → MON_ACCOUNT)...");
        backup(ds);
        addLimitColumns(ds);

        for (val row : loadLimits(ds)) {
            applyLimit(ds, row);
        }

        ds.execute("DROP TABLE MON_ACCOUNT_LIMIT");
        Logger.info("Migração de limites concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    private static boolean needsMigration(DataSource ds) {
        val count = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'MON_ACCOUNT_LIMIT'",
                AccountLimitMigration::readCount);
        return count > 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    // ── Backup + DDL estrutural ───────────────────────────────────────

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam de backup. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-account-limit-merge.zip'");
    }

    private static void addLimitColumns(DataSource ds) {
        ds.execute("ALTER TABLE MON_ACCOUNT ADD DEC_CREDIT_LIMIT DECIMAL(19, 2)");
        ds.execute("ALTER TABLE MON_ACCOUNT ADD DEC_OVERDRAFT_LIMIT DECIMAL(19, 2)");
        ds.execute("ALTER TABLE MON_ACCOUNT ADD NUM_CLOSING_DAY INT");
        ds.execute("ALTER TABLE MON_ACCOUNT ADD NUM_DUE_DAY INT");
    }

    // ── Cópia 1:1 por conta (PK igual em ambos os lados, sem colisão) ──

    private static List<LimitRow> loadLimits(DataSource ds) {
        return ds.query(
                "SELECT COD_ACCOUNT, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT, NUM_CLOSING_DAY, NUM_DUE_DAY "
                        + "FROM MON_ACCOUNT_LIMIT",
                AccountLimitMigration::readLimitRows);
    }

    private static List<LimitRow> readLimitRows(JDBCResultSet rs) {
        val rows = new ArrayList<LimitRow>();
        while (rs.next().get()) {
            rows.add(new LimitRow(
                    rs.getString("COD_ACCOUNT").get(),
                    rs.getBigDecimal("DEC_CREDIT_LIMIT").get(),
                    rs.getBigDecimal("DEC_OVERDRAFT_LIMIT").get(),
                    rs.getObject("NUM_CLOSING_DAY", Integer.class).get(),
                    rs.getObject("NUM_DUE_DAY", Integer.class).get()
            ));
        }
        return rows;
    }

    private static void applyLimit(DataSource ds, LimitRow row) {
        ds.execute(
                "UPDATE MON_ACCOUNT SET DEC_CREDIT_LIMIT = ?, DEC_OVERDRAFT_LIMIT = ?, "
                        + "NUM_CLOSING_DAY = ?, NUM_DUE_DAY = ? WHERE ID = ?",
                JDBCParameter.of(row.creditLimit(), row.overdraftLimit(), row.closingDay(), row.dueDay(), row.accountId()));
    }

    // ── Tipos auxiliares ───────────────────────────────────────────────

    @NullMarked
    private record LimitRow(
            String accountId,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay
    ) {}
}
