package br.cdb.core.persistence;

import br.cdb.AbstractUseCaseTest;
import br.cdb.core.persistence.migration.F006InstallmentDateMigration;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercita {@link F006InstallmentDateMigration} contra schemas legados montados à mão
 * num H2 em memória isolado — não toca o banco de dev real.
 */
class F006InstallmentDateMigrationUseCaseTest extends AbstractUseCaseTest {

    private static final String URL = "jdbc:h2:mem:f006installmentmigration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

    @Test
    void migratesLegacySchemaAndIsIdempotent() throws Exception {
        val avulsoId = UUID.randomUUID().toString();
        val p1Id = UUID.randomUUID().toString();
        val p2Id = UUID.randomUUID().toString();
        val p3Id = UUID.randomUUID().toString();
        val transferId1 = UUID.randomUUID().toString();
        val transferId2 = UUID.randomUUID().toString();
        val groupId = UUID.randomUUID().toString();
        val transferGroupId = UUID.randomUUID().toString();

        Class.forName("org.h2.Driver");
        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            createLegacySchema(conn);

            // Lançamento avulso
            insert(conn, avulsoId, null, "2026-05-10", 1, 1, -1);

            // Grupo de 3 parcelas com datas escalonadas
            insert(conn, p1Id, groupId, "2026-05-10", 1, 3, -1);
            insert(conn, p2Id, groupId, "2026-06-10", 2, 3, -1);
            insert(conn, p3Id, groupId, "2026-07-10", 3, 3, -1);

            // Par de transferência na forma real: mesmo groupId, TOTAL=2, sinais opostos e mesma data
            insert(conn, transferId1, transferGroupId, "2026-05-15", 1, 2, -1);
            insert(conn, transferId2, transferGroupId, "2026-05-15", 2, 2, 1);
        }

        val ds = openDataSource();
        F006InstallmentDateMigration.apply(ds);

        try (val conn = DriverManager.getConnection(URL, "sa", "")) {
            // DAT_INSTALLMENT deve existir
            assertEquals(1, countColumns(conn, "F006_TRANSACTION", "DAT_INSTALLMENT"), "coluna DAT_INSTALLMENT criada");

            // Lançamento avulso: TMS_PURCHASE inalterado, DAT_INSTALLMENT = mesma data
            assertEquals(Date.valueOf("2026-05-10"), purchasedAt(conn, avulsoId));
            assertEquals(Date.valueOf("2026-05-10"), installmentDate(conn, avulsoId));

            // Parcelas: TMS_PURCHASE deve ser o mínimo do grupo (2026-05-10), DAT_INSTALLMENT preserva escalonamento
            assertEquals(Date.valueOf("2026-05-10"), purchasedAt(conn, p1Id), "parcela 1: TMS_PURCHASE fixo");
            assertEquals(Date.valueOf("2026-05-10"), purchasedAt(conn, p2Id), "parcela 2: TMS_PURCHASE fixo (era 2026-06-10)");
            assertEquals(Date.valueOf("2026-05-10"), purchasedAt(conn, p3Id), "parcela 3: TMS_PURCHASE fixo (era 2026-07-10)");
            assertEquals(Date.valueOf("2026-05-10"), installmentDate(conn, p1Id), "parcela 1: DAT_INSTALLMENT preservado");
            assertEquals(Date.valueOf("2026-06-10"), installmentDate(conn, p2Id), "parcela 2: DAT_INSTALLMENT preservado");
            assertEquals(Date.valueOf("2026-07-10"), installmentDate(conn, p3Id), "parcela 3: DAT_INSTALLMENT preservado");

            // Par de transferência: sinais mistos excluem o grupo do passo 2 — TMS_PURCHASE inalterado
            assertEquals(Date.valueOf("2026-05-15"), purchasedAt(conn, transferId1), "transferência: TMS_PURCHASE inalterado");
            assertEquals(Date.valueOf("2026-05-15"), purchasedAt(conn, transferId2), "transferência: TMS_PURCHASE inalterado");
            assertEquals(Date.valueOf("2026-05-15"), installmentDate(conn, transferId1), "transferência: DAT_INSTALLMENT = própria data");
            assertEquals(Date.valueOf("2026-05-15"), installmentDate(conn, transferId2), "transferência: DAT_INSTALLMENT = própria data");
        }

        assertDoesNotThrow(() -> F006InstallmentDateMigration.apply(ds), "segunda chamada é no-op");
    }

    // ── wiring ───────────────────────────────────────────────────────────

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

    // ── legacy schema fixture ─────────────────────────────────────────────

    private static void createLegacySchema(Connection conn) throws Exception {
        try (val st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE F006_TRANSACTION (
                        ID CHAR(36) PRIMARY KEY,
                        GROUP_ID CHAR(36),
                        TMS_PURCHASE TIMESTAMP NOT NULL,
                        NUM_SIGNAL NUMERIC(1) NOT NULL,
                        NUM_INSTALLMENT INT NOT NULL,
                        NUM_INSTALLMENT_TOTAL INT NOT NULL
                    )
                    """);
        }
    }

    private static void insert(Connection conn, String id, String groupId, String date,
                               int installmentNumber, int totalInstallments, int signal) throws Exception {
        try (val ps = conn.prepareStatement(
                "INSERT INTO F006_TRANSACTION (ID, GROUP_ID, TMS_PURCHASE, NUM_SIGNAL, NUM_INSTALLMENT, NUM_INSTALLMENT_TOTAL) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, groupId);
            ps.setTimestamp(3, Timestamp.valueOf(date + " 00:00:00"));
            ps.setInt(4, signal);
            ps.setInt(5, installmentNumber);
            ps.setInt(6, totalInstallments);
            ps.execute();
        }
    }

    // ── assertion helpers ─────────────────────────────────────────────────

    private static int countColumns(Connection conn, String tableName, String columnName) throws Exception {
        try (val ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (val rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static Date purchasedAt(Connection conn, String id) throws Exception {
        try (val ps = conn.prepareStatement("SELECT CAST(TMS_PURCHASE AS DATE) FROM F006_TRANSACTION WHERE ID = ?")) {
            ps.setString(1, id);
            try (val rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getDate(1); }
        }
    }

    private static Date installmentDate(Connection conn, String id) throws Exception {
        try (val ps = conn.prepareStatement("SELECT DAT_INSTALLMENT FROM F006_TRANSACTION WHERE ID = ?")) {
            ps.setString(1, id);
            try (val rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getDate(1); }
        }
    }
}
