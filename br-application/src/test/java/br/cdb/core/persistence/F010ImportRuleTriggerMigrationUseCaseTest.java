package br.cdb.core.persistence;

import br.cdb.AbstractUseCaseTest;
import br.cdb.core.persistence.migration.F010ImportRuleTriggerMigration;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercita {@link F010ImportRuleTriggerMigration} contra um schema legado (TXT_NAME, sem
 * F010_IMPORT_RULE_TRIGGER) montado à mão num H2 em memória isolado — não toca o banco de dev real.
 */
class F010ImportRuleTriggerMigrationUseCaseTest extends AbstractUseCaseTest {

    private static final String URL = "jdbc:h2:mem:f010rulemigration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

    @Test
    void movesNameToLabelAndTriggerRowDropsOldColumnAndIsIdempotent() throws Exception {
        val personId = UUID.randomUUID().toString();
        val ruleId = UUID.randomUUID().toString();

        Class.forName("org.h2.Driver");
        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            createLegacySchema(conn);
            insertRule(conn, ruleId, personId, "Companhia de Saneamento");
        }

        val ds = openDataSource();
        F010ImportRuleTriggerMigration.apply(ds);

        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            assertEquals(0, countColumns(conn, "F010_IMPORT_RULE", "TXT_NAME"), "coluna antiga removida");
            assertEquals(1, countColumns(conn, "F010_IMPORT_RULE", "TXT_LABEL"), "coluna nova criada");
            assertEquals(1, countTables(conn, "F010_IMPORT_RULE_TRIGGER"), "tabela de gatilhos criada");

            assertEquals("Companhia de Saneamento", labelOf(conn, ruleId));
            assertEquals("Companhia de Saneamento", singleTriggerOf(conn, ruleId));
        }

        assertDoesNotThrow(() -> F010ImportRuleTriggerMigration.apply(ds), "segunda chamada é no-op");
    }

    // ── wiring ───────────────────────────────────────────────────────

    private static DataSource openDataSource() {
        val props = new JDBCProperties();
        props.driver("org.h2.Driver");
        props.url(URL);
        props.username("sa");
        props.password("");
        props.validationQuery("SELECT 1");
        props.minPoolSize(1);
        props.maxPoolSize(2);
        return new DataSource(props);
    }

    // ── legacy schema fixture ──────────────────────────────────────────

    private static void createLegacySchema(Connection conn) throws Exception {
        try (val st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE F010_IMPORT_RULE (
                        ID CHAR(36) PRIMARY KEY,
                        COD_PERSON CHAR(36) NOT NULL,
                        TXT_NAME VARCHAR(255) NOT NULL,
                        COD_ACCOUNT CHAR(36),
                        COD_CATEGORY CHAR(36),
                        COD_COST_CENTER CHAR(36),
                        TMS_CREATE_AT TIMESTAMP NOT NULL
                    )
                    """);
        }
    }

    private static void insertRule(Connection conn, String id, String personId, String name) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO F010_IMPORT_RULE (ID, COD_PERSON, TXT_NAME, TMS_CREATE_AT) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, personId);
            ps.setString(3, name);
            ps.setTimestamp(4, Timestamp.valueOf("2026-01-01 00:00:00"));
            ps.execute();
        }
    }

    // ── assertions helpers ─────────────────────────────────────────────

    private static int countTables(Connection conn, String tableName) throws Exception {
        try (val ps = conn.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
            ps.setString(1, tableName);
            try (val rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static int countColumns(Connection conn, String tableName, String columnName) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (val rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static String labelOf(Connection conn, String ruleId) throws Exception {
        try (val ps = conn.prepareStatement("SELECT TXT_LABEL FROM F010_IMPORT_RULE WHERE ID = ?")) {
            ps.setString(1, ruleId);
            try (val rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getString(1); }
        }
    }

    private static String singleTriggerOf(Connection conn, String ruleId) throws Exception {
        try (val ps = conn.prepareStatement("SELECT TXT_TRIGGER FROM F010_IMPORT_RULE_TRIGGER WHERE COD_RULE = ?")) {
            ps.setString(1, ruleId);
            try (val rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getString(1); }
        }
    }
}
