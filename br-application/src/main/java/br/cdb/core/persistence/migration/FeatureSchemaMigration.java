package br.cdb.core.persistence.migration;

import br.cdb.core.persistence.Database;
import br.commons.Logger;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Migração one-shot do DB de dev file-based: alcança o estado atual de {@code docs/db-features.mermaid}
 * em quatro tabelas que evoluíram juntas neste plano:
 *
 * <ul>
 *   <li>{@code SEC_USER} ganha {@code FLG_ACTIVE}/{@code TMS_CREATE_AT}/{@code TMS_UPDATED_AT}
 *       (usuários existentes viram ativos, timestamps ficam com a data da migração).</li>
 *   <li>{@code USER_ACCOUNT} perde {@code FLG_ACTIVE} (redundante com {@code MON_ACCOUNT.FLG_ACTIVE})
 *       e {@code DEC_OPENING_BALANCE} — contas com saldo inicial não-zero ganham uma
 *       {@code MON_TRANSACTION} sintética ("Saldo inicial", datada numa âncora antiga) preservando o
 *       valor antes da coluna sumir.</li>
 *   <li>{@code USER_CATEGORY.BOL_SYSTEM} vira {@code FLG_SYSTEM} (mesma convenção {@code CHAR(1)}
 *       'Y'/'N' de todo {@code FLG_*} do projeto) e {@code TXT_NATURE} vira {@code COD_NATURE}
 *       (prefixo {@code COD_} de FK de código, alinhando ao diagrama).</li>
 *   <li>{@code USER_TRANSACTION} ganha {@code COD_ACCOUNT} (retropreenchido a partir de
 *       {@code MON_TRANSACTION}) e troca a PK de {@code (COD_TRANSACTION, COD_USER)} para
 *       {@code (COD_USER, COD_ACCOUNT, COD_TRANSACTION)}.</li>
 * </ul>
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code CoreModule.dataSource(...)} logo após
 * {@link AccountLimitMigration#apply(DataSource)}. Detecta ausência de {@code SEC_USER.FLG_ACTIVE};
 * se presente — banco novo ou já migrado — não faz nada. Roda um backup online antes de qualquer
 * alteração.
 */
@NullMarked
public final class FeatureSchemaMigration {

    private static final String COST_CENTER_FIXO_ID = "d0000000-0000-0000-0000-000000000001";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String OPENING_BALANCE_DESCRIPTION = "Saldo inicial";
    private static final Timestamp ANCHOR_TIMESTAMP = Timestamp.valueOf("2000-01-01 00:00:00");
    private static final Date ANCHOR_DATE = Date.valueOf("2000-01-01");

    private FeatureSchemaMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando schema de features (SEC_USER/USER_ACCOUNT/USER_CATEGORY/USER_TRANSACTION)...");
        backup(ds);

        migrateSecUser(ds);
        migrateUserAccount(ds);
        migrateUserCategory(ds);
        migrateUserTransaction(ds);

        Logger.info("Migração de schema de features concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /**
     * Migra só quando {@code SEC_USER} já existe (banco real, legado) mas ainda não tem
     * {@code FLG_ACTIVE}. Num banco fresh, migrações rodam antes de {@link Database#model()} —
     * {@code SEC_USER} ainda não existe, então checar só a ausência da coluna nova dispararia a
     * migração contra uma tabela inexistente.
     */
    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'SEC_USER'",
                FeatureSchemaMigration::readCount);
        if (tableCount == 0) return false;

        val columnCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'SEC_USER' AND COLUMN_NAME = 'FLG_ACTIVE'",
                FeatureSchemaMigration::readCount);
        return columnCount == 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam de backup. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-feature-schema.zip'");
    }

    // ── SEC_USER: novas colunas, usuários existentes viram ativos ─────

    private static void migrateSecUser(DataSource ds) {
        ds.execute("ALTER TABLE SEC_USER ADD FLG_ACTIVE CHAR(1)");
        ds.execute("ALTER TABLE SEC_USER ADD TMS_CREATE_AT TIMESTAMP");
        ds.execute("ALTER TABLE SEC_USER ADD TMS_UPDATED_AT TIMESTAMP");

        val now = Timestamp.valueOf(Time.now());
        ds.execute(
                "UPDATE SEC_USER SET FLG_ACTIVE = 'Y', TMS_CREATE_AT = ?, TMS_UPDATED_AT = ?",
                JDBCParameter.of(now, now));

        ds.execute("ALTER TABLE SEC_USER ALTER COLUMN FLG_ACTIVE SET NOT NULL");
        ds.execute("ALTER TABLE SEC_USER ALTER COLUMN TMS_CREATE_AT SET NOT NULL");
        ds.execute("ALTER TABLE SEC_USER ALTER COLUMN TMS_UPDATED_AT SET NOT NULL");
    }

    // ── USER_ACCOUNT: saldo inicial vira transação, FLG_ACTIVE some ───

    private static void migrateUserAccount(DataSource ds) {
        for (val row : loadOpeningBalances(ds)) {
            insertOpeningBalanceTransaction(ds, row);
        }
        ds.execute("ALTER TABLE USER_ACCOUNT DROP COLUMN FLG_ACTIVE");
        ds.execute("ALTER TABLE USER_ACCOUNT DROP COLUMN DEC_OPENING_BALANCE");
    }

    private static List<OpeningBalanceRow> loadOpeningBalances(DataSource ds) {
        return ds.query(
                "SELECT COD_ACCOUNT, DEC_OPENING_BALANCE FROM USER_ACCOUNT WHERE DEC_OPENING_BALANCE <> 0",
                FeatureSchemaMigration::readOpeningBalanceRows);
    }

    private static List<OpeningBalanceRow> readOpeningBalanceRows(JDBCResultSet rs) {
        val rows = new ArrayList<OpeningBalanceRow>();
        while (rs.next().get()) {
            rows.add(new OpeningBalanceRow(rs.getString("COD_ACCOUNT").get(), rs.getBigDecimal("DEC_OPENING_BALANCE").get()));
        }
        return rows;
    }

    private static void insertOpeningBalanceTransaction(DataSource ds, OpeningBalanceRow row) {
        val amount = row.openingBalance().abs();
        val signal = row.openingBalance().signum() < 0 ? -1 : 1;
        val now = Timestamp.valueOf(Time.now());
        ds.execute(
                "INSERT INTO MON_TRANSACTION (ID, TXT_DESCRIPTION, NUM_SIGNAL, DEC_AMOUNT, TMS_PURCHASE, "
                        + "COD_ACCOUNT, COD_STATUS, COD_COST_CENTER, DAT_PAYMENT, NUM_INSTALLMENT, "
                        + "NUM_INSTALLMENT_TOTAL, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)",
                JDBCParameter.of(UUID.randomUUID().toString(), OPENING_BALANCE_DESCRIPTION, signal, amount,
                        ANCHOR_TIMESTAMP, row.accountId(), STATUS_CONFIRMED, COST_CENTER_FIXO_ID, ANCHOR_DATE,
                        now, now));
    }

    // ── USER_CATEGORY: renomeia BOL_SYSTEM → FLG_SYSTEM ───────────────

    private static void migrateUserCategory(DataSource ds) {
        ds.execute("ALTER TABLE USER_CATEGORY RENAME COLUMN BOL_SYSTEM TO FLG_SYSTEM");
        ds.execute("ALTER TABLE USER_CATEGORY RENAME COLUMN TXT_NATURE TO COD_NATURE");
    }

    // ── USER_TRANSACTION: ganha COD_ACCOUNT, PK vira (USER, ACCOUNT, TRANSACTION) ──

    private static void migrateUserTransaction(DataSource ds) {
        ds.execute("ALTER TABLE USER_TRANSACTION ADD COD_ACCOUNT CHAR(36)");
        ds.execute(
                "UPDATE USER_TRANSACTION SET COD_ACCOUNT = "
                        + "(SELECT COD_ACCOUNT FROM MON_TRANSACTION WHERE ID = COD_TRANSACTION)");
        ds.execute("ALTER TABLE USER_TRANSACTION ALTER COLUMN COD_ACCOUNT SET NOT NULL");
        ds.execute("ALTER TABLE USER_TRANSACTION DROP PRIMARY KEY");
        ds.execute("ALTER TABLE USER_TRANSACTION ADD PRIMARY KEY (COD_USER, COD_ACCOUNT, COD_TRANSACTION)");
    }

    // ── Tipos auxiliares ───────────────────────────────────────────────

    @NullMarked
    private record OpeningBalanceRow(String accountId, BigDecimal openingBalance) {}
}
