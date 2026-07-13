package br.cdb.infra.persistence.core;

import br.commons.Logger;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Migração one-shot do DB de dev file-based: um bug em {@code UserCategoryJDBCRepository.findByNature}
 * (comparava contra {@code TRANSACTION_NATURE.TXT_DESCRIPTION} em vez de {@code COD_NATURE}) fazia
 * {@code UserService.create} nunca encontrar as categorias padrão já existentes e recriar a árvore
 * inteira a cada restart do Quarkus. Esta migração funde os grupos de categorias raiz duplicadas
 * ({@code COD_USER}/{@code COD_NATURE}/{@code TXT_NAME} iguais, {@code COD_PARENT} nulo) — e,
 * transitivamente, suas filhas — mantendo sempre a cópia mais antiga e reatribuindo qualquer
 * {@code USER_TRANSACTION} vinculada às cópias descartadas antes de apagá-las.
 *
 * <p>{@link #apply(DataSource)} é chamado por {@code ContextBridge.dataSource(...)} por último na
 * cadeia, depois de {@link FeatureSchemaMigration#apply(DataSource)} — depende de {@code COD_NATURE}/
 * {@code FLG_SYSTEM} já estarem com os nomes atuais. Detecta grupos de categorias raiz duplicadas;
 * se não houver — banco novo, ou já limpo numa execução anterior — não faz nada. Roda um backup
 * online antes de qualquer alteração.
 */
@NullMarked
public final class DuplicateCategoryMigration {

    private DuplicateCategoryMigration() {}

    public static void apply(DataSource ds) {
        val groups = duplicateRootGroups(ds);
        if (groups.isEmpty()) return;

        Logger.info("Limpando categorias duplicadas (USER_CATEGORY)...");
        backup(ds);

        for (val group : groups) {
            mergeGroup(ds, group);
        }

        Logger.info("Limpeza de categorias duplicadas concluída.");
    }

    // ── Detecção ──────────────────────────────────────────────────────

    /**
     * Só categorias RAIZ ({@code COD_PARENT IS NULL}) aparecem duplicadas por um agrupamento direto:
     * cada cópia de uma raiz duplicada tem suas próprias filhas, sob um {@code COD_PARENT} diferente,
     * então filhas duplicadas nunca somam {@code COUNT(*) > 1} nesse mesmo agrupamento — são resolvidas
     * junto quando o grupo da raiz é processado, em {@link #mergeGroup}. Num banco fresh, migrações
     * rodam antes de {@link br.cdb.infra.persistence.Database#model()} — {@code USER_CATEGORY}
     * ainda não existe, daí o primeiro passo evitar consultar uma tabela inexistente.
     */
    private static List<GroupKey> duplicateRootGroups(DataSource ds) {
        val tableCount = ds.query(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'USER_CATEGORY'",
                DuplicateCategoryMigration::readCount);
        if (tableCount == 0) return List.of();

        return ds.query(
                "SELECT COD_USER, COD_NATURE, TXT_NAME FROM USER_CATEGORY "
                        + "WHERE COD_PARENT IS NULL "
                        + "GROUP BY COD_USER, COD_NATURE, TXT_NAME HAVING COUNT(*) > 1",
                DuplicateCategoryMigration::readGroupKeys);
    }

    private static long readCount(JDBCResultSet rs) {
        rs.next().get();
        return rs.getLong(1).get();
    }

    private static List<GroupKey> readGroupKeys(JDBCResultSet rs) {
        val keys = new ArrayList<GroupKey>();
        while (rs.next().get()) {
            keys.add(new GroupKey(
                    rs.getString("COD_USER").get(),
                    rs.getString("COD_NATURE").get(),
                    rs.getString("TXT_NAME").get()));
        }
        return keys;
    }

    /** {@code BACKUP TO} exige banco persistente — bancos em memória (perfil de teste) não precisam de backup. */
    private static void backup(DataSource ds) {
        if (ds.properties().url().contains(":mem:")) return;
        ds.execute("BACKUP TO './database-pre-category-dedup.zip'");
    }

    // ── Fusão de um grupo de raízes duplicadas ────────────────────────

    private static void mergeGroup(DataSource ds, GroupKey key) {
        val roots = loadRoots(ds, key);
        val survivorRoot = roots.get(0);
        val loserRoots = roots.subList(1, roots.size());
        val allRootIds = roots.stream().map(CategoryRow::id).toList();

        // Filhas de TODAS as cópias primeiro — só depois as raízes perdedoras são apagadas. Invertida,
        // uma raiz perdedora com filhas ainda não fundidas cascatearia (COD_PARENT ON DELETE CASCADE) e
        // apagaria essas filhas silenciosamente, sem erro de FK, em vez de preservá-las na sobrevivente.
        mergeChildren(ds, allRootIds, survivorRoot.id());

        for (val loserRoot : loserRoots) {
            reassignTransactions(ds, loserRoot.id(), survivorRoot.id());
            deleteCategory(ds, loserRoot.id());
        }
    }

    private static List<CategoryRow> loadRoots(DataSource ds, GroupKey key) {
        return ds.query(
                "SELECT ID, TXT_NAME, COD_PARENT FROM USER_CATEGORY "
                        + "WHERE COD_PARENT IS NULL AND COD_USER = ? AND COD_NATURE = ? AND TXT_NAME = ? "
                        + "ORDER BY TMS_CREATE_AT ASC, ID ASC",
                JDBCParameter.of(key.userId(), key.nature(), key.name()),
                DuplicateCategoryMigration::readCategoryRows);
    }

    // ── Fusão das filhas de todas as cópias do grupo, por nome ────────

    private static void mergeChildren(DataSource ds, List<String> rootIds, String survivorRootId) {
        val childrenByName = loadChildren(ds, rootIds).stream()
                .collect(Collectors.groupingBy(CategoryRow::name, LinkedHashMap::new, Collectors.toList()));

        for (val sameNameChildren : childrenByName.values()) {
            mergeChildGroup(ds, sameNameChildren, survivorRootId);
        }
    }

    private static List<CategoryRow> loadChildren(DataSource ds, List<String> rootIds) {
        val placeholders = rootIds.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        return ds.query(
                "SELECT ID, TXT_NAME, COD_PARENT FROM USER_CATEGORY "
                        + "WHERE COD_PARENT IN (" + placeholders + ") "
                        + "ORDER BY TMS_CREATE_AT ASC, ID ASC",
                JDBCParameter.of(rootIds.toArray()),
                DuplicateCategoryMigration::readCategoryRows);
    }

    /**
     * {@code sameNameChildren} já vem ordenada por criação (mais antiga primeiro) — ela sobrevive.
     * Um nome sem duplicata (grupo de tamanho 1) ainda pode precisar reparentar: se sua única cópia
     * mora sob uma raiz perdedora, ela é movida para a raiz sobrevivente antes que a perdedora suma.
     */
    private static void mergeChildGroup(DataSource ds, List<CategoryRow> sameNameChildren, String survivorRootId) {
        val survivor = sameNameChildren.get(0);
        for (val loser : sameNameChildren.subList(1, sameNameChildren.size())) {
            reassignTransactions(ds, loser.id(), survivor.id());
            deleteCategory(ds, loser.id());
        }
        if (!survivorRootId.equals(survivor.parentId())) {
            reparent(ds, survivor.id(), survivorRootId);
        }
    }

    // ── Primitivas de leitura/escrita ──────────────────────────────────

    private static List<CategoryRow> readCategoryRows(JDBCResultSet rs) {
        val rows = new ArrayList<CategoryRow>();
        while (rs.next().get()) {
            rows.add(new CategoryRow(
                    rs.getString("ID").get(),
                    rs.getString("TXT_NAME").get(),
                    rs.getString("COD_PARENT").get()));
        }
        return rows;
    }

    private static void reassignTransactions(DataSource ds, String oldCategoryId, String newCategoryId) {
        ds.execute(
                "UPDATE USER_TRANSACTION SET COD_CATEGORY = ? WHERE COD_CATEGORY = ?",
                JDBCParameter.of(newCategoryId, oldCategoryId));
    }

    private static void reparent(DataSource ds, String categoryId, String newParentId) {
        ds.execute(
                "UPDATE USER_CATEGORY SET COD_PARENT = ? WHERE ID = ?",
                JDBCParameter.of(newParentId, categoryId));
    }

    private static void deleteCategory(DataSource ds, String categoryId) {
        ds.execute("DELETE FROM USER_CATEGORY WHERE ID = ?", JDBCParameter.of(categoryId));
    }

    // ── Tipos auxiliares ───────────────────────────────────────────────

    @NullMarked
    private record GroupKey(String userId, String nature, String name) {}

    @NullMarked
    private record CategoryRow(String id, String name, @Nullable String parentId) {}
}
