package br.cdb.infra.persistence.core;

import br.commons.Logger;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Migração one-shot do DB de dev file-based: dissolve os contextos {@code br-context-monetary}/
 * {@code br-context-people} no schema por-fatia de {@code docs/db-features.mermaid} — renomeia
 * {@code MON_*}/{@code PEP_*} para {@code FNNN_*}/{@code SYS_*}, funde {@code PERSON_ACCOUNT} em
 * {@code F002_ACCOUNT} e o resto de {@code PERSON_TRANSACTION} em {@code F006_TRANSACTION}, extrai
 * {@code F005_TRANSACTION_CATEGORY} da coluna {@code PERSON_TRANSACTION.COD_CATEGORY}, cria o usuário
 * SYSTEM com as 2 categorias de transferência globais e reatribui os vínculos existentes a elas.
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code ContextBridge.dataSource(...)} por último na
 * cadeia — depende do schema final de {@link FeatureSchemaMigration}/{@link DuplicateCategoryMigration}.
 * Detecta a presença de {@code PEP_PERSON} (marcador antigo); banco novo ou já migrado não tem essa
 * tabela nesse ponto (migrações rodam antes de {@code Database.model()}), então não faz nada. Roda um
 * backup online antes de qualquer alteração.
 *
 * <p>{@code F002_ACCOUNT.COD_PERSON}/{@code TXT_COLOR} ficam nullable de propósito: a criação de conta
 * continua em dois passos (contexto grava a conta, feature grava dono/cor por cima do mesmo registro) até
 * a fatia f002 assumir o INSERT único — endurecer para {@code NOT NULL} é tarefa de fase futura, não
 * desta migração.
 */
@NullMarked
public final class ContextMergeMigration {

    private static final String SYSTEM_PERSON_ID = "f9990000-0000-0000-0000-000000000001";
    private static final String TRANSFER_CATEGORY_EXPENSE_ID = "f9990000-0000-0000-0000-000000000002";
    private static final String TRANSFER_CATEGORY_INCOME_ID = "f9990000-0000-0000-0000-000000000003";
    private static final String TRANSFER_MACRO_NAME = "9. Outros";
    private static final String TRANSFER_CATEGORY_NAME = "Transferência";

    private ContextMergeMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;

        Logger.info("Fundindo contextos (MON_*/PEP_*/PERSON_* -> FNNN_*/SYS_*)...");
        backup(ds);

        renameSimpleTables(ds);
        mergeAccount(ds);
        mergeCard(ds);
        extractTransactionCategory(ds);
        mergeTransaction(ds);
        createDeletionQueue(ds);
        seedSystemTransferCategories(ds);

        Logger.info("Fusão de contextos concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /**
     * Migra só quando {@code PEP_PERSON} (marcador antigo) ainda existe. Num banco fresh, migrações
     * rodam antes de {@link br.cdb.infra.persistence.Database#model()} — {@code PEP_PERSON} ainda não
     * existe, então a contagem já vem zero e não dispara nada contra tabelas inexistentes.
     */
    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PEP_PERSON'",
                ContextMergeMigration::readCount);
        return tableCount > 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam de backup. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-context-merge.zip'");
    }

    // ── Renomeações simples (1 tabela → 1 tabela, mesma forma) ─────────

    private static void renameSimpleTables(DataSource ds) {
        ds.execute("ALTER TABLE PEP_PERSON RENAME TO F000_PERSON");
        ds.execute("ALTER TABLE SEC_USER RENAME TO F000_USER");
        ds.execute("ALTER TABLE USER_CREDENTIAL RENAME TO F000_USER_CREDENTIAL");
        ds.execute("ALTER TABLE PERSON_PREFERENCES RENAME TO F000_PREFERENCES");
        ds.execute("ALTER TABLE MON_COST_CENTER RENAME TO F000_COST_CENTER");
        ds.execute("ALTER TABLE MON_ACCOUNT_TYPE RENAME TO SYS_ACCOUNT_TYPE");
        ds.execute("ALTER TABLE MON_STATUS RENAME TO SYS_STATUS");
        ds.execute("ALTER TABLE TRANSACTION_NATURE RENAME TO SYS_TRANSACTION_NATURE");
        ds.execute("ALTER TABLE PERSON_TAG RENAME TO F004_TAG");
        ds.execute("ALTER TABLE PERSON_TRANSACTION_TAG RENAME TO F004_TRANSACTION_TAG");
        ds.execute("ALTER TABLE PERSON_CATEGORY RENAME TO F005_CATEGORY");

        ds.execute("ALTER TABLE PERSON_ACCOUNT_BALANCE RENAME TO F002_BALANCE");
        ds.execute("ALTER TABLE F002_BALANCE ADD FLG_DIRTY CHAR(1)");
        ds.execute("UPDATE F002_BALANCE SET FLG_DIRTY = 'N'");
        ds.execute("ALTER TABLE F002_BALANCE ALTER COLUMN FLG_DIRTY SET NOT NULL");
    }

    // ── F002_ACCOUNT = MON_ACCOUNT + PERSON_ACCOUNT ────────────────────

    private static void mergeAccount(DataSource ds) {
        ds.execute("ALTER TABLE MON_ACCOUNT RENAME TO F002_ACCOUNT");
        ds.execute("ALTER TABLE F002_ACCOUNT ADD COD_PERSON CHAR(36)");
        ds.execute("ALTER TABLE F002_ACCOUNT ADD TXT_COLOR VARCHAR(20)");
        ds.execute(
                "UPDATE F002_ACCOUNT SET "
                        + "COD_PERSON = (SELECT PA.COD_PERSON FROM PERSON_ACCOUNT PA WHERE PA.COD_ACCOUNT = F002_ACCOUNT.ID), "
                        + "TXT_COLOR = (SELECT PA.TXT_COLOR FROM PERSON_ACCOUNT PA WHERE PA.COD_ACCOUNT = F002_ACCOUNT.ID)");
        ds.execute("DROP TABLE PERSON_ACCOUNT");
    }

    // ── F003_CARD = MON_CARD + COD_PERSON derivado da conta ────────────

    private static void mergeCard(DataSource ds) {
        ds.execute("ALTER TABLE MON_CARD RENAME TO F003_CARD");
        ds.execute("ALTER TABLE F003_CARD ADD COD_PERSON CHAR(36)");
        ds.execute(
                "UPDATE F003_CARD SET COD_PERSON = "
                        + "(SELECT A.COD_PERSON FROM F002_ACCOUNT A WHERE A.ID = F003_CARD.COD_ACCOUNT)");
    }

    // ── F005_TRANSACTION_CATEGORY extraído de PERSON_TRANSACTION.COD_CATEGORY ──

    private static void extractTransactionCategory(DataSource ds) {
        ds.execute(
                """
                CREATE TABLE F005_TRANSACTION_CATEGORY (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_CATEGORY CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON)
                )
                """);
        ds.execute(
                "INSERT INTO F005_TRANSACTION_CATEGORY (COD_TRANSACTION, COD_PERSON, COD_CATEGORY) "
                        + "SELECT COD_TRANSACTION, COD_PERSON, COD_CATEGORY FROM PERSON_TRANSACTION");
    }

    // ── F006_TRANSACTION = MON_TRANSACTION + COD_PERSON de PERSON_TRANSACTION ──

    private static void mergeTransaction(DataSource ds) {
        ds.execute("ALTER TABLE MON_TRANSACTION RENAME TO F006_TRANSACTION");
        ds.execute("ALTER TABLE F006_TRANSACTION ADD COD_PERSON CHAR(36)");
        ds.execute(
                "UPDATE F006_TRANSACTION SET COD_PERSON = "
                        + "(SELECT PT.COD_PERSON FROM PERSON_TRANSACTION PT WHERE PT.COD_TRANSACTION = F006_TRANSACTION.ID)");
        ds.execute("DROP TABLE PERSON_TRANSACTION");
    }

    private static void createDeletionQueue(DataSource ds) {
        ds.execute(
                """
                CREATE TABLE F999_DELETION_QUEUE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_TYPE VARCHAR(40) NOT NULL,
                    COD_TARGET CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    NUM_ATTEMPTS INT NOT NULL,
                    FLG_LOCKED CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """);
    }

    // ── Usuário SYSTEM + categorias de transferência globais ───────────

    /**
     * A categoria de transferência deixa de ser por pessoa: cria o {@code F000_PERSON} SYSTEM e uma
     * categoria raiz global por natureza (EXPENSE/INCOME), depois reatribui todo vínculo em
     * {@code F005_TRANSACTION_CATEGORY} que apontava para a antiga categoria "Transferência" por-pessoa
     * (reconhecida pela forma pré-fix: {@code FLG_SYSTEM='Y'}, nome "Transferência", pai raiz
     * "9. Outros") para a global equivalente, e remove a cópia por-pessoa. Esta migração só limpa o
     * histórico existente — {@code UserCategoryService.findOrCreateTransferCategory} já lê/semeia
     * direto sob estes IDs fixos, sem depender dela para bancos fresh/teste.
     */
    private static void seedSystemTransferCategories(DataSource ds) {
        val now = Timestamp.valueOf(Time.now());
        ds.execute(
                "INSERT INTO F000_PERSON (ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                JDBCParameter.of(SYSTEM_PERSON_ID, "SYSTEM", "pt-BR", "pt-BR", now, now));

        insertGlobalTransferCategory(ds, TRANSFER_CATEGORY_EXPENSE_ID, "EXPENSE", now);
        insertGlobalTransferCategory(ds, TRANSFER_CATEGORY_INCOME_ID, "INCOME", now);

        for (val legacy : findLegacyTransferCategories(ds)) {
            val globalId = "EXPENSE".equals(legacy.nature()) ? TRANSFER_CATEGORY_EXPENSE_ID : TRANSFER_CATEGORY_INCOME_ID;
            ds.execute(
                    "UPDATE F005_TRANSACTION_CATEGORY SET COD_CATEGORY = ? WHERE COD_CATEGORY = ?",
                    JDBCParameter.of(globalId, legacy.id()));
            ds.execute("DELETE FROM F005_CATEGORY WHERE ID = ?", JDBCParameter.of(legacy.id()));
        }
    }

    private static void insertGlobalTransferCategory(DataSource ds, String id, String nature, Timestamp now) {
        ds.execute(
                "INSERT INTO F005_CATEGORY "
                        + "(ID, COD_PERSON, COD_PARENT, COD_NATURE, TXT_NAME, FLG_SYSTEM, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, NULL, ?, ?, 'Y', 'Y', ?, ?)",
                JDBCParameter.of(id, SYSTEM_PERSON_ID, nature, TRANSFER_CATEGORY_NAME, now, now));
    }

    private static List<LegacyCategoryRow> findLegacyTransferCategories(DataSource ds) {
        return ds.query(
                "SELECT C.ID, C.COD_NATURE FROM F005_CATEGORY C "
                        + "JOIN F005_CATEGORY P ON P.ID = C.COD_PARENT "
                        + "WHERE C.TXT_NAME = ? AND C.FLG_SYSTEM = 'Y' AND P.TXT_NAME = ? AND P.COD_PARENT IS NULL",
                JDBCParameter.of(TRANSFER_CATEGORY_NAME, TRANSFER_MACRO_NAME),
                ContextMergeMigration::readLegacyCategoryRows);
    }

    private static List<LegacyCategoryRow> readLegacyCategoryRows(JDBCResultSet rs) {
        val rows = new ArrayList<LegacyCategoryRow>();
        while (rs.next().get()) {
            rows.add(new LegacyCategoryRow(rs.getString("ID").get(), rs.getString("COD_NATURE").get()));
        }
        return rows;
    }

    // ── Tipos auxiliares ───────────────────────────────────────────────

    @NullMarked
    private record LegacyCategoryRow(String id, String nature) {}
}
