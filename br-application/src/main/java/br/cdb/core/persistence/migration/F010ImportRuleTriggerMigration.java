package br.cdb.core.persistence.migration;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Migração one-shot: {@code F010_IMPORT_RULE.TXT_NAME} fazia papel duplo (padrão de match E texto
 * de substituição da descrição). Vira {@code TXT_LABEL} (rótulo, sem função de match) + uma linha
 * por gatilho em {@code F010_IMPORT_RULE_TRIGGER} (nova tabela) — o valor antigo de TXT_NAME de
 * cada regra vira seu único gatilho inicial.
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code F010Module.initialize()}, antes de
 * {@code Database.initialize(model())} da própria fatia — o {@code model()} novo já declara o
 * schema no formato final (sem TXT_NAME, com TXT_LABEL, com F010_IMPORT_RULE_TRIGGER), então essa
 * migração precisa deixar o banco legado nesse formato antes do model() confirmar.
 */
@NullMarked
public final class F010ImportRuleTriggerMigration {

    private F010ImportRuleTriggerMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Migrando F010_IMPORT_RULE (TXT_NAME → TXT_LABEL + F010_IMPORT_RULE_TRIGGER)...");
        backup(ds);

        ds.execute("ALTER TABLE F010_IMPORT_RULE ADD TXT_LABEL VARCHAR(255)");
        ds.execute("UPDATE F010_IMPORT_RULE SET TXT_LABEL = TXT_NAME");
        ds.execute("ALTER TABLE F010_IMPORT_RULE ALTER COLUMN TXT_LABEL SET NOT NULL");

        ds.execute("""
                CREATE TABLE F010_IMPORT_RULE_TRIGGER (
                    COD_RULE CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_TRIGGER VARCHAR(255) NOT NULL,
                    PRIMARY KEY (COD_RULE, TXT_TRIGGER)
                )
                """);
        ds.execute("INSERT INTO F010_IMPORT_RULE_TRIGGER (COD_RULE, COD_PERSON, TXT_TRIGGER) "
                + "SELECT ID, COD_PERSON, TXT_NAME FROM F010_IMPORT_RULE");

        ds.execute("ALTER TABLE F010_IMPORT_RULE DROP COLUMN TXT_NAME");

        Logger.info("Migração F010_IMPORT_RULE concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /** Só migra se a tabela legada já existe E ainda tem a coluna antiga — nunca dispara num banco
     *  fresh (tabela ainda não criada pelo model() novo) nem roda 2x num banco já migrado. */
    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'F010_IMPORT_RULE'",
                F010ImportRuleTriggerMigration::readCount);
        if (tableCount == 0) return false;

        val columnCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'F010_IMPORT_RULE' AND COLUMN_NAME = 'TXT_NAME'",
                F010ImportRuleTriggerMigration::readCount);
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
        ds.execute("BACKUP TO './database-pre-f010-import-rule-trigger.zip'");
    }
}
