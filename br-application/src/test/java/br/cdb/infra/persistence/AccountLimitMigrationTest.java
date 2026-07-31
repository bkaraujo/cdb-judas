package br.cdb.infra.persistence;

import br.cdb.core.persistence.migration.AccountLimitMigration;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercita {@link AccountLimitMigration} contra um schema legado (pré-merge, com
 * {@code MON_ACCOUNT_LIMIT} própria) montado à mão num H2 em memória isolado — não toca o banco de
 * dev real. Cenário: conta A com limite completo; conta B sem nenhuma linha de limite.
 */
class AccountLimitMigrationTest {

    private static final String URL = "jdbc:h2:mem:accountlimitmigration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    private static final String CHECKING_TYPE_ID = "a1000000-0000-0000-0000-000000000001";

    @Test
    void mergesLimitIntoAccountDropsOldTableAndIsIdempotent() throws Exception {
        val accountA = UUID.randomUUID().toString();
        val accountB = UUID.randomUUID().toString();

        Class.forName("org.h2.Driver");
        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            createLegacySchema(conn);
            insertAccount(conn, accountA, "Conta A");
            insertAccount(conn, accountB, "Conta B");
            insertLimit(conn, accountA, "1000.00", "200.00", 5, 10);
        }

        val ds = openDataSource();
        AccountLimitMigration.apply(ds);

        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            assertEquals(0, countTables(conn, "MON_ACCOUNT_LIMIT"), "tabela antiga removida");

            assertEquals(0, new BigDecimal("1000.00").compareTo(accountColumn(conn, accountA, "DEC_CREDIT_LIMIT")));
            assertEquals(0, new BigDecimal("200.00").compareTo(accountColumn(conn, accountA, "DEC_OVERDRAFT_LIMIT")));
            assertEquals(5, accountColumnInt(conn, accountA, "NUM_CLOSING_DAY"));
            assertEquals(10, accountColumnInt(conn, accountA, "NUM_DUE_DAY"));

            assertNull(accountColumn(conn, accountB, "DEC_CREDIT_LIMIT"), "conta sem linha de limite fica nula");
        }

        assertDoesNotThrow(() -> AccountLimitMigration.apply(ds), "segunda chamada é no-op");
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
                    CREATE TABLE MON_ACCOUNT_TYPE (
                        ID CHAR(36) PRIMARY KEY,
                        TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL
                    )
                    """);
            st.execute("INSERT INTO MON_ACCOUNT_TYPE VALUES ('" + CHECKING_TYPE_ID + "', 'Conta corrente', 'Y')");

            st.execute("""
                    CREATE TABLE MON_ACCOUNT (
                        ID CHAR(36) PRIMARY KEY,
                        TXT_NAME VARCHAR(255) NOT NULL,
                        TXT_TYPE CHAR(36) NOT NULL REFERENCES MON_ACCOUNT_TYPE(ID),
                        FLG_ACTIVE CHAR(1) NOT NULL,
                        TMS_CREATE_AT TIMESTAMP NOT NULL,
                        TMS_UPDATED_AT TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
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
        }
    }

    private static void insertAccount(Connection conn, String id, String name) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO MON_ACCOUNT (ID, TXT_NAME, TXT_TYPE, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, 'Y', ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-01 00:00:00");
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, CHECKING_TYPE_ID);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.execute();
        }
    }

    private static void insertLimit(Connection conn, String accountId, String creditLimit, String overdraftLimit,
                                     int closingDay, int dueDay) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO MON_ACCOUNT_LIMIT (COD_ACCOUNT, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT, "
                        + "NUM_CLOSING_DAY, NUM_DUE_DAY, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-01 00:00:00");
            ps.setString(1, accountId);
            ps.setBigDecimal(2, new BigDecimal(creditLimit));
            ps.setBigDecimal(3, new BigDecimal(overdraftLimit));
            ps.setInt(4, closingDay);
            ps.setInt(5, dueDay);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.execute();
        }
    }

    // ── assertions helpers ─────────────────────────────────────────────

    private static int countTables(Connection conn, String tableName) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
            ps.setString(1, tableName);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static BigDecimal accountColumn(Connection conn, String accountId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM MON_ACCOUNT WHERE ID = ?")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next(), "conta " + accountId + " deve existir");
                return rs.getBigDecimal(1);
            }
        }
    }

    private static int accountColumnInt(Connection conn, String accountId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM MON_ACCOUNT WHERE ID = ?")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
