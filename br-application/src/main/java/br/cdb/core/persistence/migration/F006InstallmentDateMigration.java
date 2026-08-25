package br.cdb.core.persistence.migration;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Migração one-shot: adiciona {@code DAT_INSTALLMENT DATE NOT NULL} a {@code F006_TRANSACTION} e
 * re-semantiza {@code TMS_PURCHASE} como data real de compra (fixa no grupo). Detecta pela
 * <b>ausência</b> da nova coluna (diferente das migrações que detectam presença de coluna antiga).
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code F006Module.initialize()} <b>antes</b> de
 * {@code Database.initialize(model())} — garante migração antes da validação do novo schema.
 */
@NullMarked
public final class F006InstallmentDateMigration {

    private F006InstallmentDateMigration() {}

    public static void apply(DataSource ds) {
        if (!needsMigration(ds)) return;
        Logger.info("Migrando F006_TRANSACTION (TMS_PURCHASE fixa + DAT_INSTALLMENT)...");
        backup(ds);

        ds.execute("ALTER TABLE F006_TRANSACTION ADD DAT_INSTALLMENT DATE");

        // 1) Captura o TMS_PURCHASE ATUAL (já deslocado por parcela) em DAT_INSTALLMENT, pra TODA linha —
        //    preserva 100% o bucketing de fatura/saldo já gravado. TEM que rodar ANTES do passo 2.
        ds.execute("UPDATE F006_TRANSACTION SET DAT_INSTALLMENT = CAST(TMS_PURCHASE AS DATE)");

        // 2) Só DEPOIS fixa TMS_PURCHASE ao valor mais antigo do grupo, só pra grupos de parcelamento
        //    reais. Lançamentos avulsos (GROUP_ID nulo) ficam de fora. Par de transferência também
        //    tem GROUP_ID e NUM_INSTALLMENT_TOTAL = 2 — o que o separa de um parcelamento é a mistura
        //    de sinais (uma perna +1, outra -1), o mesmo critério de TransactionService
        //    .findTransferSiblings; o NOT EXISTS exclui esses grupos.
        //    MIN() em vez de "parcela #1 especificamente" — a #1 pode ter sido apagada isoladamente.
        ds.execute("""
                UPDATE F006_TRANSACTION t
                SET TMS_PURCHASE = (
                    SELECT MIN(s.TMS_PURCHASE) FROM F006_TRANSACTION s WHERE s.GROUP_ID = t.GROUP_ID
                )
                WHERE t.GROUP_ID IS NOT NULL AND t.NUM_INSTALLMENT_TOTAL > 1
                  AND NOT EXISTS (
                      SELECT 1 FROM F006_TRANSACTION x
                      WHERE x.GROUP_ID = t.GROUP_ID AND x.NUM_SIGNAL <> t.NUM_SIGNAL
                  )
                """);

        ds.execute("ALTER TABLE F006_TRANSACTION ALTER COLUMN DAT_INSTALLMENT SET NOT NULL");
        Logger.info("Migração F006_TRANSACTION (DAT_INSTALLMENT) concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /** Só migra se a tabela já existe E a coluna nova ainda não existe. */
    private static boolean needsMigration(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'F006_TRANSACTION'",
                F006InstallmentDateMigration::readCount);
        if (tableCount == 0) return false;
        val columnCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'F006_TRANSACTION' AND COLUMN_NAME = 'DAT_INSTALLMENT'",
                F006InstallmentDateMigration::readCount);
        return columnCount == 0;
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    // ── Backup ────────────────────────────────────────────────────────

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-f006-installment-date.zip'");
    }
}
