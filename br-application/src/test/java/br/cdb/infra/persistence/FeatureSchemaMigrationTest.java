package br.cdb.infra.persistence;

import br.cdb.infra.persistence.core.FeatureSchemaMigration;
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
 * Exercita {@link FeatureSchemaMigration} contra um schema legado (pré-migração de features) montado
 * à mão num H2 em memória isolado — não toca o banco de dev real. Cenário: conta A com saldo inicial
 * 500 (deve virar transação sintética); conta B sem saldo inicial (não deve gerar transação);
 * categoria de sistema; uma transação/categoria existentes pra checar o retropreenchimento de
 * USER_TRANSACTION.COD_ACCOUNT.
 */
class FeatureSchemaMigrationTest {

    private static final String URL = "jdbc:h2:mem:featureschemamigration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    private static final String CHECKING_TYPE_ID = "a1000000-0000-0000-0000-000000000001";

    @Test
    void migratesSecUserUserAccountUserCategoryAndUserTransactionAndIsIdempotent() throws Exception {
        val userId = UUID.randomUUID().toString();
        val accountA = UUID.randomUUID().toString();
        val accountB = UUID.randomUUID().toString();
        val categoryId = UUID.randomUUID().toString();
        val txId = UUID.randomUUID().toString();

        Class.forName("org.h2.Driver");
        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            createLegacySchema(conn);
            insertAccount(conn, accountA, "Conta A");
            insertAccount(conn, accountB, "Conta B");
            insertUser(conn, userId, "tester");
            insertUserAccount(conn, userId, accountA, "500.00");
            insertUserAccount(conn, userId, accountB, "0.00");
            insertCategory(conn, categoryId, userId, "N");
            insertTransaction(conn, txId, accountA);
            insertUserTransaction(conn, txId, userId, categoryId);
        }

        val ds = openDataSource();
        FeatureSchemaMigration.apply(ds);

        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            assertEquals(1, columnExists(conn, "SEC_USER", "FLG_ACTIVE"));
            assertEquals("Y", secUserColumn(conn, userId, "FLG_ACTIVE"));
            assertNotNull(secUserTimestamp(conn, userId, "TMS_CREATE_AT"));

            assertEquals(0, columnExists(conn, "USER_ACCOUNT", "FLG_ACTIVE"), "FLG_ACTIVE removida de USER_ACCOUNT");
            assertEquals(0, columnExists(conn, "USER_ACCOUNT", "DEC_OPENING_BALANCE"), "DEC_OPENING_BALANCE removida");

            assertEquals(1, openingBalanceTransactionCount(conn, accountA), "conta A ganha transação sintética");
            assertEquals(0, openingBalanceTransactionCount(conn, accountB), "conta B sem saldo inicial não gera transação");
            val synthetic = openingBalanceTransaction(conn, accountA);
            assertEquals(0, new BigDecimal("500.00").compareTo(synthetic.amount()));
            assertEquals(1, synthetic.signal());

            assertEquals(1, columnExists(conn, "USER_CATEGORY", "FLG_SYSTEM"));
            assertEquals(0, columnExists(conn, "USER_CATEGORY", "BOL_SYSTEM"));
            assertEquals(1, columnExists(conn, "USER_CATEGORY", "COD_NATURE"));
            assertEquals(0, columnExists(conn, "USER_CATEGORY", "TXT_NATURE"));

            assertEquals(1, columnExists(conn, "USER_TRANSACTION", "COD_ACCOUNT"));
            assertEquals(accountA, userTransactionAccountId(conn, txId), "COD_ACCOUNT retropreenchido do MON_TRANSACTION");
        }

        assertDoesNotThrow(() -> FeatureSchemaMigration.apply(ds), "segunda chamada é no-op");
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
                    CREATE TABLE MON_STATUS (
                        ID VARCHAR(20) PRIMARY KEY,
                        TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL
                    )
                    """);
            st.execute("INSERT INTO MON_STATUS VALUES ('CONFIRMED', 'Confirmado', 'Y')");
            st.execute("""
                    CREATE TABLE MON_COST_CENTER (
                        ID CHAR(36) PRIMARY KEY,
                        TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL,
                        TMS_CREATE_AT TIMESTAMP NOT NULL,
                        TMS_UPDATED_AT TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("INSERT INTO MON_COST_CENTER VALUES ('d0000000-0000-0000-0000-000000000001', 'Fixo', 'Y', "
                    + "TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-01-01 00:00:00')");
            st.execute("""
                    CREATE TABLE MON_TRANSACTION (
                        ID CHAR(36) PRIMARY KEY,
                        TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                        NUM_SIGNAL INT NOT NULL,
                        DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                        TMS_PURCHASE TIMESTAMP NOT NULL,
                        COD_ACCOUNT CHAR(36) NOT NULL,
                        COD_STATUS VARCHAR(20) NOT NULL REFERENCES MON_STATUS(ID),
                        COD_COST_CENTER CHAR(36) NOT NULL,
                        DAT_PAYMENT DATE,
                        GROUP_ID CHAR(36),
                        NUM_INSTALLMENT INT NOT NULL,
                        NUM_INSTALLMENT_TOTAL INT NOT NULL,
                        TXT_NOTES VARCHAR(1000),
                        TMS_CREATE_AT TIMESTAMP NOT NULL,
                        TMS_UPDATED_AT TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE TRANSACTION_NATURE (
                        ID VARCHAR(20) PRIMARY KEY,
                        TXT_DESCRIPTION VARCHAR(50) NOT NULL
                    )
                    """);
            st.execute("INSERT INTO TRANSACTION_NATURE VALUES ('EXPENSE', 'Despesa')");

            st.execute("""
                    CREATE TABLE SEC_USER (
                        ID CHAR(36) PRIMARY KEY,
                        TXT_USERNAME VARCHAR(255) NOT NULL,
                        COD_PERSON CHAR(36) NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE USER_ACCOUNT (
                        COD_USER CHAR(36) NOT NULL,
                        COD_ACCOUNT CHAR(36) NOT NULL,
                        DEC_OPENING_BALANCE DECIMAL(19, 2) NOT NULL,
                        TXT_COLOR VARCHAR(20) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL,
                        PRIMARY KEY (COD_USER, COD_ACCOUNT)
                    )
                    """);
            st.execute("""
                    CREATE TABLE USER_CATEGORY (
                        ID CHAR(36) PRIMARY KEY,
                        COD_USER CHAR(36) NOT NULL,
                        TXT_NATURE VARCHAR(20) NOT NULL REFERENCES TRANSACTION_NATURE(ID),
                        TXT_NAME VARCHAR(255) NOT NULL,
                        COD_PARENT CHAR(36),
                        BOL_SYSTEM CHAR(1) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL,
                        TMS_CREATE_AT TIMESTAMP NOT NULL,
                        TMS_UPDATED_AT TIMESTAMP NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE USER_TRANSACTION (
                        COD_TRANSACTION CHAR(36) NOT NULL,
                        COD_USER CHAR(36) NOT NULL,
                        COD_CATEGORY CHAR(36),
                        TMS_CREATE_AT TIMESTAMP NOT NULL,
                        TMS_UPDATED_AT TIMESTAMP NOT NULL,
                        PRIMARY KEY (COD_TRANSACTION, COD_USER)
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

    private static void insertUser(Connection conn, String id, String username) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO SEC_USER (ID, TXT_USERNAME, COD_PERSON) VALUES (?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, username);
            ps.setString(3, UUID.randomUUID().toString());
            ps.execute();
        }
    }

    private static void insertUserAccount(Connection conn, String userId, String accountId, String openingBalance) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO USER_ACCOUNT (COD_USER, COD_ACCOUNT, DEC_OPENING_BALANCE, TXT_COLOR, FLG_ACTIVE) "
                        + "VALUES (?, ?, ?, '#000000', 'Y')")) {
            ps.setString(1, userId);
            ps.setString(2, accountId);
            ps.setBigDecimal(3, new BigDecimal(openingBalance));
            ps.execute();
        }
    }

    private static void insertCategory(Connection conn, String id, String userId, String bolSystem) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO USER_CATEGORY (ID, COD_USER, TXT_NATURE, TXT_NAME, COD_PARENT, BOL_SYSTEM, FLG_ACTIVE, "
                        + "TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, 'EXPENSE', 'Casa', NULL, ?, 'Y', ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-01 00:00:00");
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, bolSystem);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.execute();
        }
    }

    private static void insertTransaction(Connection conn, String id, String accountId) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO MON_TRANSACTION (ID, TXT_DESCRIPTION, NUM_SIGNAL, DEC_AMOUNT, TMS_PURCHASE, "
                        + "COD_ACCOUNT, COD_STATUS, COD_COST_CENTER, NUM_INSTALLMENT, NUM_INSTALLMENT_TOTAL, "
                        + "TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, 'Compra', -1, 50.00, ?, ?, 'CONFIRMED', "
                        + "'d0000000-0000-0000-0000-000000000001', 1, 1, ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-05 00:00:00");
            ps.setString(1, id);
            ps.setTimestamp(2, now);
            ps.setString(3, accountId);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.execute();
        }
    }

    private static void insertUserTransaction(Connection conn, String txId, String userId, String categoryId) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO USER_TRANSACTION (COD_TRANSACTION, COD_USER, COD_CATEGORY, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-05 00:00:00");
            ps.setString(1, txId);
            ps.setString(2, userId);
            ps.setString(3, categoryId);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.execute();
        }
    }

    // ── assertions helpers ─────────────────────────────────────────────

    private static int columnExists(Connection conn, String table, String column) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String secUserColumn(Connection conn, String userId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM SEC_USER WHERE ID = ?")) {
            ps.setString(1, userId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    private static Timestamp secUserTimestamp(Connection conn, String userId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM SEC_USER WHERE ID = ?")) {
            ps.setString(1, userId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getTimestamp(1);
            }
        }
    }

    private static int openingBalanceTransactionCount(Connection conn, String accountId) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM MON_TRANSACTION WHERE COD_ACCOUNT = ? AND TXT_DESCRIPTION = 'Saldo inicial'")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private record SyntheticTx(BigDecimal amount, int signal) {}

    private static SyntheticTx openingBalanceTransaction(Connection conn, String accountId) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT DEC_AMOUNT, NUM_SIGNAL FROM MON_TRANSACTION WHERE COD_ACCOUNT = ? AND TXT_DESCRIPTION = 'Saldo inicial'")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return new SyntheticTx(rs.getBigDecimal(1), rs.getInt(2));
            }
        }
    }

    private static String userTransactionAccountId(Connection conn, String txId) throws Exception {
        try (val ps = conn.prepareStatement("SELECT COD_ACCOUNT FROM USER_TRANSACTION WHERE COD_TRANSACTION = ?")) {
            ps.setString(1, txId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }
}
