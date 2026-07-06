package br.community.infra.persistence.core;

import br.commons.Logger;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.infra.persistence.Database;
import br.community.infra.persistence.monetary.AccountTypeMapper;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Migração one-shot do DB de dev file-based: cartão deixou de ser {@code MON_ACCOUNT} do tipo
 * {@code CREDIT_CARD} e virou entidade própria ({@code MON_CARD}, identificada só pelo last4,
 * vinculada a uma conta real); limites/ciclo de fatura saíram do overlay {@code USER_ACCOUNT} para
 * {@code MON_ACCOUNT_LIMIT} (linha única por conta).
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code ContextBridge.dataSource(...)} antes de
 * {@link Database#model()} rodar. Detecta se o schema é o legado (coluna
 * {@code USER_ACCOUNT.TXT_CARD_LAST4} ainda presente); se não for — banco novo ou já migrado —
 * não faz nada. Roda um backup online (arquivo {@code .zip}) antes de qualquer alteração.
 */
@NullMarked
public final class LegacyCardMigration {

    private static final String CREDIT_CARD_TYPE_ID = "a1000000-0000-0000-0000-000000000003";

    private static final String OVERLAY_COLUMNS =
            "COD_LINKED_ACCOUNT, TXT_CARD_LAST4, NUM_DUE_DAY, NUM_CLOSING_DAY, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT";

    private static final List<String> LEGACY_OVERLAY_COLUMNS = List.of(
            "COD_LINKED_ACCOUNT", "TXT_CARD_LAST4", "NUM_DUE_DAY", "NUM_CLOSING_DAY",
            "DEC_CREDIT_LIMIT", "DEC_OVERDRAFT_LIMIT");

    private LegacyCardMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando dados legados de cartão (USER_ACCOUNT → MON_CARD/MON_ACCOUNT_LIMIT)...");
        backup(ds);
        createNewTables(ds);

        val limits = new LinkedHashMap<String, LimitAcc>();
        collectRealAccountLimits(ds, limits);
        for (val cardAccountId : loadCreditCardAccountIds(ds)) {
            migrateCardAccount(ds, cardAccountId, limits);
        }
        flushLimits(ds, limits);

        dropLegacyOverlayColumns(ds);
        Logger.info("Migração de cartões concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    private static boolean needsMigration(DataSource ds) {
        val count = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'USER_ACCOUNT' AND COLUMN_NAME = 'TXT_CARD_LAST4'",
                LegacyCardMigration::readCount);
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
        ds.execute("BACKUP TO './database-pre-card-remodel.zip'");
    }

    private static void createNewTables(DataSource ds) {
        ds.execute("""
                CREATE TABLE MON_CARD (
                    ID CHAR(36) PRIMARY KEY,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    TXT_LAST4 CHAR(4) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """);
        ds.execute("""
                CREATE TABLE MON_ACCOUNT_LIMIT (
                    COD_ACCOUNT CHAR(36) PRIMARY KEY,
                    DEC_CREDIT_LIMIT DECIMAL(19, 2),
                    DEC_OVERDRAFT_LIMIT DECIMAL(19, 2),
                    NUM_CLOSING_DAY INT,
                    NUM_DUE_DAY INT,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """);
        ds.execute("ALTER TABLE MON_TRANSACTION ADD COD_CARD CHAR(36)");
    }

    // ── Migração por conta-cartão ─────────────────────────────────────

    private static List<String> loadCreditCardAccountIds(DataSource ds) {
        return ds.query(
                "SELECT ID FROM MON_ACCOUNT WHERE TXT_TYPE = ?",
                JDBCParameter.of(CREDIT_CARD_TYPE_ID),
                LegacyCardMigration::readIdList);
    }

    private static List<String> readIdList(JDBCResultSet rs) {
        val ids = new ArrayList<String>();
        while (rs.next().get()) ids.add(rs.getString("ID").get());
        return ids;
    }

    private static void migrateCardAccount(DataSource ds, String cardAccountId, Map<String, LimitAcc> limits) {
        val overlay = loadOverlay(ds, cardAccountId);
        if (overlay == null) {
            convertToChecking(ds, cardAccountId);
            return;
        }

        val linked = overlay.linkedAccountId();
        val target = linked != null ? linked : cardAccountId;
        mergeLimit(limits, target, overlay);
        val cardId = overlay.last4() != null ? insertCard(ds, target, overlay.last4()) : null;

        if (linked == null) {
            convertToChecking(ds, cardAccountId);
            if (cardId != null) tagTransactionsWithCard(ds, cardAccountId, cardId);
            return;
        }

        repointTransactions(ds, cardAccountId, target, cardId);
        deleteStaleBalanceSnapshots(ds, target);
        deleteDeadCardAccount(ds, cardAccountId);
    }

    private static void convertToChecking(DataSource ds, String accountId) {
        ds.execute("UPDATE MON_ACCOUNT SET TXT_TYPE = ? WHERE ID = ?",
                JDBCParameter.of(AccountTypeMapper.CHECKING_ID, accountId));
    }

    private static void repointTransactions(DataSource ds, String fromAccountId, String toAccountId, @Nullable String cardId) {
        ds.execute("UPDATE MON_TRANSACTION SET COD_ACCOUNT = ?, COD_CARD = ? WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(toAccountId, cardId, fromAccountId));
    }

    private static void tagTransactionsWithCard(DataSource ds, String accountId, String cardId) {
        ds.execute("UPDATE MON_TRANSACTION SET COD_CARD = ? WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(cardId, accountId));
    }

    private static void deleteDeadCardAccount(DataSource ds, String accountId) {
        ds.execute("DELETE FROM USER_ACCOUNT_BALANCE WHERE COD_ACCOUNT = ?", JDBCParameter.of(accountId));
        ds.execute("DELETE FROM USER_ACCOUNT WHERE COD_ACCOUNT = ?", JDBCParameter.of(accountId));
        ds.execute("DELETE FROM MON_ACCOUNT WHERE ID = ?", JDBCParameter.of(accountId));
    }

    private static void deleteStaleBalanceSnapshots(DataSource ds, String accountId) {
        ds.execute("DELETE FROM USER_ACCOUNT_BALANCE WHERE COD_ACCOUNT = ?", JDBCParameter.of(accountId));
    }

    // ── Cartão: insere (dedupe por conta+last4) ───────────────────────

    private static String insertCard(DataSource ds, String targetAccountId, String last4) {
        val existing = findExistingCardId(ds, targetAccountId, last4);
        if (existing != null) return existing;

        val id = UUID.randomUUID().toString();
        val now = Timestamp.valueOf(Time.now());
        ds.execute(
                "INSERT INTO MON_CARD (ID, TXT_LAST4, COD_ACCOUNT, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, 'Y', ?, ?)",
                JDBCParameter.of(id, last4, targetAccountId, now, now));
        return id;
    }

    private static @Nullable String findExistingCardId(DataSource ds, String accountId, String last4) {
        return ds.query(
                "SELECT ID FROM MON_CARD WHERE COD_ACCOUNT = ? AND TXT_LAST4 = ?",
                JDBCParameter.of(accountId, last4),
                LegacyCardMigration::readOptionalId);
    }

    private static @Nullable String readOptionalId(JDBCResultSet rs) {
        return rs.next().get() ? rs.getString("ID").get() : null;
    }

    // ── Overlay (linha antiga de USER_ACCOUNT) ────────────────────────

    private static @Nullable Overlay loadOverlay(DataSource ds, String accountId) {
        return ds.query(
                "SELECT " + OVERLAY_COLUMNS + " FROM USER_ACCOUNT WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId),
                LegacyCardMigration::readFirstOverlay);
    }

    private static @Nullable Overlay readFirstOverlay(JDBCResultSet rs) {
        return rs.next().get() ? toOverlay(rs) : null;
    }

    private static Overlay toOverlay(JDBCResultSet rs) {
        val linked = blankToNull(rs.getString("COD_LINKED_ACCOUNT").get());
        val last4 = blankToNull(rs.getString("TXT_CARD_LAST4").get());
        val dueDay = rs.getObject("NUM_DUE_DAY", Integer.class).get();
        val closingDay = rs.getObject("NUM_CLOSING_DAY", Integer.class).get();
        val creditLimit = rs.getBigDecimal("DEC_CREDIT_LIMIT").get();
        val overdraftLimit = rs.getBigDecimal("DEC_OVERDRAFT_LIMIT").get();
        return new Overlay(linked, last4, dueDay, closingDay, creditLimit, overdraftLimit);
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ── Limites: conta real com overlay preenchido, MAX quando duplicado ──

    private static void collectRealAccountLimits(DataSource ds, Map<String, LimitAcc> limits) {
        val cardAccountIds = loadCreditCardAccountIds(ds);
        for (val row : loadAllOverlays(ds)) {
            if (!cardAccountIds.contains(row.accountId()) && row.overlay().hasAnyLimit()) {
                mergeLimit(limits, row.accountId(), row.overlay());
            }
        }
    }

    private static List<OverlayRow> loadAllOverlays(DataSource ds) {
        return ds.query(
                "SELECT COD_ACCOUNT, " + OVERLAY_COLUMNS + " FROM USER_ACCOUNT",
                LegacyCardMigration::readOverlayRows);
    }

    private static List<OverlayRow> readOverlayRows(JDBCResultSet rs) {
        val rows = new ArrayList<OverlayRow>();
        while (rs.next().get()) {
            rows.add(new OverlayRow(rs.getString("COD_ACCOUNT").get(), toOverlay(rs)));
        }
        return rows;
    }

    private static void mergeLimit(Map<String, LimitAcc> limits, String accountId, Overlay overlay) {
        if (!overlay.hasAnyLimit()) return;
        val acc = limits.computeIfAbsent(accountId, ignored -> new LimitAcc());
        acc.creditLimit = maxOf(acc.creditLimit, overlay.creditLimit());
        acc.overdraftLimit = maxOf(acc.overdraftLimit, overlay.overdraftLimit());
        if (acc.closingDay == null) acc.closingDay = overlay.closingDay();
        if (acc.dueDay == null) acc.dueDay = overlay.dueDay();
    }

    private static @Nullable BigDecimal maxOf(@Nullable BigDecimal a, @Nullable BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.max(b);
    }

    private static void flushLimits(DataSource ds, Map<String, LimitAcc> limits) {
        val now = Timestamp.valueOf(Time.now());
        for (val entry : limits.entrySet()) {
            val acc = entry.getValue();
            ds.execute(
                    "INSERT INTO MON_ACCOUNT_LIMIT (COD_ACCOUNT, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT, "
                            + "NUM_CLOSING_DAY, NUM_DUE_DAY, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of(entry.getKey(), acc.creditLimit, acc.overdraftLimit, acc.closingDay, acc.dueDay, now, now));
        }
    }

    // ── Corte final: colunas legadas + seed do tipo ───────────────────

    private static void dropLegacyOverlayColumns(DataSource ds) {
        for (val column : LEGACY_OVERLAY_COLUMNS) {
            ds.execute("ALTER TABLE USER_ACCOUNT DROP COLUMN " + column);
        }
        ds.execute("DELETE FROM MON_ACCOUNT_TYPE WHERE ID = ?", JDBCParameter.of(CREDIT_CARD_TYPE_ID));
    }

    // ── Tipos auxiliares ───────────────────────────────────────────────

    @NullMarked
    private record Overlay(
            @Nullable String linkedAccountId,
            @Nullable String last4,
            @Nullable Integer dueDay,
            @Nullable Integer closingDay,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit
    ) {
        boolean hasAnyLimit() {
            return creditLimit != null || overdraftLimit != null || closingDay != null || dueDay != null;
        }
    }

    @NullMarked
    private record OverlayRow(String accountId, Overlay overlay) {}

    @NullMarked
    private static final class LimitAcc {
        @Nullable BigDecimal creditLimit;
        @Nullable BigDecimal overdraftLimit;
        @Nullable Integer closingDay;
        @Nullable Integer dueDay;
    }
}
