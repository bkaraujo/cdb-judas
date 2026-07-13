package br.cdb.infra.persistence;

import br.cdb.infra.persistence.core.LegacyCardMigration;
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
 * Exercita {@link LegacyCardMigration} contra um schema legado (pré-{@code MON_CARD}) montado à mão
 * num H2 em memória isolado — não toca o banco de dev real. Cenário: conta real A; cartão C vinculado
 * a A (last4, limites, dias); cartão D sem vínculo (vira conta real ele mesmo); transações em ambos.
 */
class LegacyCreditCardMigrationTest {

    private static final String URL = "jdbc:h2:mem:legacycardmigration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    private static final String CHECKING_TYPE_ID = "a1000000-0000-0000-0000-000000000001";
    private static final String CREDIT_CARD_TYPE_ID = "a1000000-0000-0000-0000-000000000003";

    @Test
    void migratesLinkedAndUnlinkedCardsMergesLimitsAndIsIdempotent() throws Exception {
        val accountA = UUID.randomUUID().toString();
        val cardC = UUID.randomUUID().toString();
        val cardD = UUID.randomUUID().toString();
        val txOnC = UUID.randomUUID().toString();
        val txOnD = UUID.randomUUID().toString();

        Class.forName("org.h2.Driver");
        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            createLegacySchema(conn);
            seedAccounts(conn, accountA, cardC, cardD);
            seedCardOverlay(conn, cardC, accountA, "1234", 5, 10, "2000.00", "300.00");
            seedCardOverlay(conn, cardD, null, "5678", 1, 15, "500.00", null);
            seedTransaction(conn, txOnC, cardC);
            seedTransaction(conn, txOnD, cardD);
            seedBalanceSnapshot(conn, accountA);
        }

        val ds = openDataSource();
        LegacyCardMigration.apply(ds);

        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            assertEquals(0, countColumns(conn, "TXT_CARD_LAST4"), "coluna legada removida");
            assertFalse(accountExists(conn, cardC), "conta-cartão vinculada é apagada");
            assertEquals(CHECKING_TYPE_ID, accountType(conn, cardD), "cartão sem vínculo vira conta real (CHECKING)");
            assertEquals(0, countRows(conn, "MON_ACCOUNT_TYPE", "ID = '" + CREDIT_CARD_TYPE_ID + "'"), "seed do tipo removido");

            assertEquals(accountA, cardAccountId(conn, "1234"));
            assertEquals(cardD, cardAccountId(conn, "5678"));

            assertEquals(accountA, transactionAccountId(conn, txOnC), "transação do cartão vinculado repontada para a conta real");
            assertEquals(cardId(conn, "1234"), transactionCardId(conn, txOnC));
            assertEquals(cardD, transactionAccountId(conn, txOnD), "transação do cartão auto-convertido fica na própria conta");
            assertEquals(cardId(conn, "5678"), transactionCardId(conn, txOnD));

            assertEquals(0, new BigDecimal("2000.00").compareTo(accountLimit(conn, accountA, "DEC_CREDIT_LIMIT")));
            assertEquals(0, new BigDecimal("300.00").compareTo(accountLimit(conn, accountA, "DEC_OVERDRAFT_LIMIT")));
            assertEquals(5, accountLimitInt(conn, accountA, "NUM_CLOSING_DAY"));
            assertEquals(10, accountLimitInt(conn, accountA, "NUM_DUE_DAY"));

            assertEquals(0, new BigDecimal("500.00").compareTo(accountLimit(conn, cardD, "DEC_CREDIT_LIMIT")));
            assertEquals(1, accountLimitInt(conn, cardD, "NUM_CLOSING_DAY"));

            assertEquals(0, countRows(conn, "USER_ACCOUNT_BALANCE", "COD_ACCOUNT = '" + accountA + "'"),
                    "snapshot antigo da conta que recebeu transações reapontadas é descartado");
        }

        assertDoesNotThrow(() -> LegacyCardMigration.apply(ds), "segunda chamada é no-op");
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
            st.execute("INSERT INTO MON_ACCOUNT_TYPE VALUES ('a1000000-0000-0000-0000-000000000002', 'Investimento', 'Y')");
            st.execute("INSERT INTO MON_ACCOUNT_TYPE VALUES ('" + CREDIT_CARD_TYPE_ID + "', 'Cartão de crédito', 'Y')");

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
                    CREATE TABLE USER_ACCOUNT (
                        COD_USER CHAR(36) NOT NULL,
                        COD_ACCOUNT CHAR(36) NOT NULL,
                        DEC_OPENING_BALANCE DECIMAL(19, 2) NOT NULL,
                        TXT_COLOR VARCHAR(20) NOT NULL,
                        FLG_ACTIVE CHAR(1) NOT NULL,
                        COD_LINKED_ACCOUNT CHAR(36),
                        TXT_CARD_LAST4 VARCHAR(4),
                        NUM_DUE_DAY INT,
                        NUM_CLOSING_DAY INT,
                        DEC_CREDIT_LIMIT DECIMAL(19, 2),
                        DEC_OVERDRAFT_LIMIT DECIMAL(19, 2),
                        PRIMARY KEY (COD_USER, COD_ACCOUNT)
                    )
                    """);
            st.execute("""
                    CREATE TABLE USER_ACCOUNT_BALANCE (
                        ID CHAR(36) PRIMARY KEY,
                        COD_USER CHAR(36) NOT NULL,
                        COD_ACCOUNT CHAR(36) NOT NULL,
                        NUM_PERIOD INT NOT NULL,
                        DEC_BALANCE DECIMAL(19, 2) NOT NULL
                    )
                    """);
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
        }
    }

    private static void seedAccounts(Connection conn, String accountA, String cardC, String cardD) throws Exception {
        insertAccount(conn, accountA, "Conta Corrente", CHECKING_TYPE_ID);
        insertAccount(conn, cardC, "Cartão C", CREDIT_CARD_TYPE_ID);
        insertAccount(conn, cardD, "Cartão D", CREDIT_CARD_TYPE_ID);
    }

    private static void insertAccount(Connection conn, String id, String name, String typeId) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO MON_ACCOUNT (ID, TXT_NAME, TXT_TYPE, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, 'Y', ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-01 00:00:00");
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, typeId);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.execute();
        }
    }

    private static void seedCardOverlay(Connection conn, String cardAccountId, String linkedAccountId,
                                         String last4, Integer closingDay, Integer dueDay,
                                         String creditLimit, String overdraftLimit) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO USER_ACCOUNT (COD_USER, COD_ACCOUNT, DEC_OPENING_BALANCE, TXT_COLOR, FLG_ACTIVE, "
                        + "COD_LINKED_ACCOUNT, TXT_CARD_LAST4, NUM_DUE_DAY, NUM_CLOSING_DAY, DEC_CREDIT_LIMIT, DEC_OVERDRAFT_LIMIT) "
                        + "VALUES ('user-1', ?, 0, '#000000', 'Y', ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, cardAccountId);
            ps.setString(2, linkedAccountId);
            ps.setString(3, last4);
            ps.setObject(4, dueDay);
            ps.setObject(5, closingDay);
            ps.setBigDecimal(6, creditLimit == null ? null : new BigDecimal(creditLimit));
            ps.setBigDecimal(7, overdraftLimit == null ? null : new BigDecimal(overdraftLimit));
            ps.execute();
        }
    }

    private static void seedTransaction(Connection conn, String id, String accountId) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO MON_TRANSACTION (ID, TXT_DESCRIPTION, NUM_SIGNAL, DEC_AMOUNT, TMS_PURCHASE, "
                        + "COD_ACCOUNT, COD_STATUS, COD_COST_CENTER, NUM_INSTALLMENT, NUM_INSTALLMENT_TOTAL, "
                        + "TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, 'Compra', -1, 50.00, ?, ?, 'CONFIRMED', ?, 1, 1, ?, ?)")) {
            val now = Timestamp.valueOf("2026-01-05 00:00:00");
            ps.setString(1, id);
            ps.setTimestamp(2, now);
            ps.setString(3, accountId);
            ps.setString(4, UUID.randomUUID().toString());
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            ps.execute();
        }
    }

    private static void seedBalanceSnapshot(Connection conn, String accountId) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO USER_ACCOUNT_BALANCE (ID, COD_USER, COD_ACCOUNT, NUM_PERIOD, DEC_BALANCE) "
                        + "VALUES (?, 'user-1', ?, 202601, 1000.00)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, accountId);
            ps.execute();
        }
    }

    // ── assertions helpers ─────────────────────────────────────────────

    private static int countColumns(Connection conn, String columnName) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'USER_ACCOUNT' AND COLUMN_NAME = ?")) {
            ps.setString(1, columnName);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int countRows(Connection conn, String table, String where) throws Exception {
        try (val st = conn.createStatement(); val rs = st.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean accountExists(Connection conn, String accountId) throws Exception {
        return countRows(conn, "MON_ACCOUNT", "ID = '" + accountId + "'") > 0;
    }

    private static String accountType(Connection conn, String accountId) throws Exception {
        try (val ps = conn.prepareStatement("SELECT TXT_TYPE FROM MON_ACCOUNT WHERE ID = ?")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static String cardAccountId(Connection conn, String last4) throws Exception {
        try (val ps = conn.prepareStatement("SELECT COD_ACCOUNT FROM MON_CARD WHERE TXT_LAST4 = ?")) {
            ps.setString(1, last4);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next(), "cartão com last4 " + last4 + " deve existir");
                return rs.getString(1);
            }
        }
    }

    private static String cardId(Connection conn, String last4) throws Exception {
        try (val ps = conn.prepareStatement("SELECT ID FROM MON_CARD WHERE TXT_LAST4 = ?")) {
            ps.setString(1, last4);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next(), "cartão com last4 " + last4 + " deve existir");
                return rs.getString(1);
            }
        }
    }

    private static String transactionAccountId(Connection conn, String txId) throws Exception {
        return transactionColumn(conn, txId, "COD_ACCOUNT");
    }

    private static String transactionCardId(Connection conn, String txId) throws Exception {
        return transactionColumn(conn, txId, "COD_CARD");
    }

    private static String transactionColumn(Connection conn, String txId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM MON_TRANSACTION WHERE ID = ?")) {
            ps.setString(1, txId);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static BigDecimal accountLimit(Connection conn, String accountId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM MON_ACCOUNT_LIMIT WHERE COD_ACCOUNT = ?")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                assertTrue(rs.next(), "limite da conta " + accountId + " deve existir");
                return rs.getBigDecimal(1);
            }
        }
    }

    private static int accountLimitInt(Connection conn, String accountId, String column) throws Exception {
        try (val ps = conn.prepareStatement("SELECT " + column + " FROM MON_ACCOUNT_LIMIT WHERE COD_ACCOUNT = ?")) {
            ps.setString(1, accountId);
            try (val rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
