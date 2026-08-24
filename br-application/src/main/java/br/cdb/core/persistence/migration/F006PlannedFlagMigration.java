package br.cdb.core.persistence.migration;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Migração one-shot: `F006_TRANSACTION.COD_COST_CENTER` (FK) vira `FLG_PLANNED` (booleano).
 * `F010_IMPORT_RULE.COD_COST_CENTER` também vira `FLG_PLANNED` (nullable — permite "não mexe na flag").
 * Tabela `F000_COST_CENTER` é removida.
 *
 * <p>{@link #apply(DataSource)} é chamado por `F006Module.initialize()` **antes** de
 * `Database.initialize(model())` da própria fatia — garante que o schema legado é migrado para
 * o novo formato antes do model() confirmar.
 */
@NullMarked
public final class F006PlannedFlagMigration {

    private F006PlannedFlagMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando F006_TRANSACTION (COD_COST_CENTER → FLG_PLANNED)...");
        backup(ds);

        // UUID de Fixo: 'd0000000-0000-0000-0000-000000000001'
        ds.execute("ALTER TABLE F006_TRANSACTION ADD FLG_PLANNED CHAR(1)");
        ds.execute("UPDATE F006_TRANSACTION SET FLG_PLANNED = CASE WHEN COD_COST_CENTER = 'd0000000-0000-0000-0000-000000000001' THEN 'Y' ELSE 'N' END");
        ds.execute("ALTER TABLE F006_TRANSACTION ALTER COLUMN FLG_PLANNED SET NOT NULL");
        ds.execute("ALTER TABLE F006_TRANSACTION DROP COLUMN COD_COST_CENTER");

        ds.execute("ALTER TABLE F010_IMPORT_RULE ADD FLG_PLANNED CHAR(1)");
        ds.execute("UPDATE F010_IMPORT_RULE SET FLG_PLANNED = CASE WHEN COD_COST_CENTER IS NULL THEN NULL WHEN COD_COST_CENTER = 'd0000000-0000-0000-0000-000000000001' THEN 'Y' ELSE 'N' END");
        ds.execute("ALTER TABLE F010_IMPORT_RULE DROP COLUMN COD_COST_CENTER");

        ds.execute("DROP TABLE F000_COST_CENTER");

        Logger.info("Migração F006_TRANSACTION concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /** Só migra se a tabela legada já existe E ainda tem a coluna antiga — nunca dispara num banco
     *  fresh (tabela ainda não criada pelo model() novo) nem roda 2x num banco já migrado. */
    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'F006_TRANSACTION'",
                F006PlannedFlagMigration::readCount);
        if (tableCount == 0) return false;

        val columnCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'F006_TRANSACTION' AND COLUMN_NAME = 'COD_COST_CENTER'",
                F006PlannedFlagMigration::readCount);
        return columnCount > 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    // ── Backup ────────────────────────────────────────────────────────

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-f006-planned-flag.zip'");
    }
}
