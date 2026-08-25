package br.cdb.core.persistence.migration;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Migração one-shot: `F006_TRANSACTION.NUM_SIGNAL` vira `FLG_REVERSAL` (booleano).
 */
@NullMarked
public final class F006ReversalFlagMigration {

    private F006ReversalFlagMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando F006_TRANSACTION (NUM_SIGNAL -> FLG_REVERSAL)...");
        backup(ds);

        ds.execute("ALTER TABLE F006_TRANSACTION ADD FLG_REVERSAL CHAR(1)");
        
        // Se NUM_SIGNAL for > 0 e a categoria for EXPENSE, é um estorno ('Y').
        // Se NUM_SIGNAL for < 0 e a categoria for INCOME, é um estorno ('Y').
        // Caso contrário, é 'N'.
        ds.execute("""
            UPDATE F006_TRANSACTION t
            SET FLG_REVERSAL = (
                SELECT CASE 
                    WHEN t.NUM_SIGNAL > 0 AND c.COD_NATURE = 'EXPENSE' THEN 'Y'
                    WHEN t.NUM_SIGNAL < 0 AND c.COD_NATURE = 'INCOME' THEN 'Y'
                    ELSE 'N' 
                END
                FROM F006_TRANSACTION_CATEGORY tc
                JOIN F005_CATEGORY c ON tc.COD_CATEGORY = c.ID
                WHERE tc.COD_TRANSACTION = t.ID
            )
        """);
        
        // Registros órfãos (sem categoria no momento) ficarão nulos no subselect,
        // então caem no null default, precisamos lidar com isso.
        ds.execute("UPDATE F006_TRANSACTION SET FLG_REVERSAL = 'N' WHERE FLG_REVERSAL IS NULL");
        
        ds.execute("ALTER TABLE F006_TRANSACTION ALTER COLUMN FLG_REVERSAL SET NOT NULL");
        ds.execute("ALTER TABLE F006_TRANSACTION DROP COLUMN NUM_SIGNAL");

        Logger.info("Migração F006_TRANSACTION (FLG_REVERSAL) concluída.");
    }

    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'F006_TRANSACTION'",
                F006ReversalFlagMigration::readCount);
        if (tableCount == 0) return false;

        val columnCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'F006_TRANSACTION' AND COLUMN_NAME = 'NUM_SIGNAL'",
                F006ReversalFlagMigration::readCount);
        return columnCount > 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-f006-reversal-flag.zip'");
    }
}
