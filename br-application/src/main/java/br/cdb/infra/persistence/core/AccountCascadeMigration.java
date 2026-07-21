package br.cdb.infra.persistence.core;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Migração one-shot do DB de dev file-based: {@code PERSON_ACCOUNT.COD_ACCOUNT} e
 * {@code PERSON_TRANSACTION.COD_ACCOUNT} (FKs para {@code MON_ACCOUNT}) ganham
 * {@code ON DELETE CASCADE} — completa o que {@code PERSON_TRANSACTION.COD_TRANSACTION}/
 * {@code PERSON_TRANSACTION_TAG.COD_TRANSACTION} (para {@code MON_TRANSACTION}) já tinham, deixando
 * a exclusão de conta consistente com a de transação: o contexto apaga a conta, os overlays de
 * feature somem em cascata (ver {@code .claude/refactor.md}, catálogo de eventos).
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code ContextBridge.dataSource(...)} depois de
 * {@link DuplicateCategoryMigration#apply(DataSource)}. H2 não nomeia a constraint no
 * {@code REFERENCES} inline do {@code CREATE TABLE} (nome auto-gerado, ex. {@code CONSTRAINT_3D}),
 * então o nome atual é descoberto via {@code INFORMATION_SCHEMA} antes do
 * {@code DROP}+{@code ADD CONSTRAINT}. Detecta se {@code PERSON_ACCOUNT} já existe (banco real,
 * legado) mas sua FK para {@code MON_ACCOUNT} ainda não é {@code CASCADE}; num banco fresh a tabela
 * ainda não existe quando esta migração roda (antes de {@code Database.model()}), então não faz
 * nada — o {@code CREATE TABLE} já nasce com a constraint certa. Roda um backup online antes de
 * qualquer alteração.
 */
@NullMarked
public final class AccountCascadeMigration {

    private AccountCascadeMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando FKs de conta para ON DELETE CASCADE (PERSON_ACCOUNT/PERSON_TRANSACTION)...");
        backup(ds);

        recreateAsCascade(ds, "PERSON_ACCOUNT", "COD_ACCOUNT", "FK_PERSON_ACCOUNT_ACCOUNT");
        recreateAsCascade(ds, "PERSON_TRANSACTION", "COD_ACCOUNT", "FK_PERSON_TRANSACTION_ACCOUNT");

        Logger.info("Migração de FKs de conta concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PERSON_ACCOUNT'",
                AccountCascadeMigration::readCount);
        if (tableCount == 0) return false;

        return !isCascade(ds, "PERSON_ACCOUNT", "COD_ACCOUNT");
    }

    private static boolean isCascade(DataSource ds, String table, String column) {
        val rule = ds.query(
                "SELECT rc.DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc "
                        + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME "
                        + "WHERE kcu.TABLE_NAME = ? AND kcu.COLUMN_NAME = ?",
                JDBCParameter.of(table, column),
                AccountCascadeMigration::readDeleteRule);
        return "CASCADE".equals(rule);
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    private static @Nullable String readDeleteRule(JDBCResultSet rs) {
        if (!rs.next().get()) return null;
        return rs.getString("DELETE_RULE").get();
    }

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam de backup. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-account-cascade.zip'");
    }

    // ── DDL ─────────────────────────────────────────────────────────────

    /**
     * Descobre o nome atual da constraint (auto-gerado, sem padrão previsível) e a recria com
     * {@code ON DELETE CASCADE} sob um nome estável.
     */
    private static void recreateAsCascade(DataSource ds, String table, String column, String newConstraintName) {
        val existingName = ds.query(
                "SELECT rc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc "
                        + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME "
                        + "WHERE kcu.TABLE_NAME = ? AND kcu.COLUMN_NAME = ?",
                JDBCParameter.of(table, column),
                AccountCascadeMigration::readConstraintName);
        if (existingName == null) {
            Logger.warn("Constraint de %s.%s não encontrada — pulando (schema inesperado).", table, column);
            return;
        }

        ds.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + existingName);
        ds.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + newConstraintName
                + " FOREIGN KEY (" + column + ") REFERENCES MON_ACCOUNT(ID) ON DELETE CASCADE");
    }

    private static @Nullable String readConstraintName(JDBCResultSet rs) {
        if (!rs.next().get()) return null;
        return rs.getString("CONSTRAINT_NAME").get();
    }
}
