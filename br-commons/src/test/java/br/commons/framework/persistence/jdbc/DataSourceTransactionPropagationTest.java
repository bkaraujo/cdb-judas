package br.commons.framework.persistence.jdbc;

import br.commons.Result;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Propagação de transação (REQUIRED): um bloco aninhado participa da transação em curso na thread —
 * não commita, não reverte e não devolve a conexão ao pool. Antes desta semântica, a primeira operação
 * aninhada (uma leitura, um {@code save} de repositório ou a introspecção de um {@code JDBCRepository})
 * fechava a conexão da transação externa e desligava o slot da thread, partindo um bloco
 * {@code transaction(...)} em N transações autônomas — ver {@code f999} (criação de usuário).
 */
class DataSourceTransactionPropagationTest {

    private static JDBCProperties props(String dbName) {
        var p = new JDBCProperties();
        p.name(dbName);
        p.driver("org.h2.Driver");
        p.url("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        p.username("sa");
        p.password("");
        p.autoCommit(false);
        p.validationQuery("SELECT 1");
        p.minPoolSize(1);
        p.maxPoolSize(4);
        p.connectionTimeout(3000);
        return p;
    }

    private static DataSource dataSource(String dbName) {
        var ds = new DataSource(props(dbName));
        ds.execute("CREATE TABLE T (ID BIGINT PRIMARY KEY)");
        return ds;
    }

    private static long count(DataSource ds) {
        return ds.query("SELECT COUNT(*) FROM T", rs -> {
            rs.next().get();
            return rs.getLong(1).get();
        });
    }

    /** Conta a partir de outra thread — logo, de outra conexão: só vê o que já foi commitado. */
    private static long countFromAnotherConnection(DataSource ds) {
        var seen = new AtomicLong(-1);
        var thread = Thread.ofPlatform().start(() -> seen.set(count(ds)));
        try {
            thread.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("leitura noutra conexão interrompida");
        }
        assertFalse(thread.isAlive(), "a leitura noutra conexão não devia bloquear");
        return seen.get();
    }

    /** Escrita aninhada só fica visível a outra conexão quando a transação externa commita. */
    @Test
    void nestedWriteCommitsOnlyWithTheOutermostTransaction() {
        var ds = dataSource("txpropwrite");
        try {
            ds.transaction(_ -> {
                ds.execute("INSERT INTO T VALUES (1)"); // mutating aninhado: participa, não commita
                assertEquals(0, countFromAnotherConnection(ds),
                        "escrita aninhada não pode ser commitada antes do fim da transação externa");
                return Result.success(true);
            });

            assertEquals(1, countFromAnotherConnection(ds),
                    "o commit do nível mais externo tem de publicar o trabalho aninhado");
        } finally {
            ds.close();
        }
    }

    /** Leitura aninhada não pode reverter nem devolver ao pool a conexão da transação externa. */
    @Test
    void nestedReadKeepsTheOutermostConnection() {
        var ds = dataSource("txpropread");
        try {
            ds.transaction(_ -> {
                ds.execute("INSERT INTO T VALUES (1)");
                assertEquals(1, ds.getActiveConnections());

                assertEquals(1, count(ds), "a leitura aninhada tem de enxergar o trabalho em curso");
                assertEquals(1, ds.getActiveConnections(),
                        "leitura aninhada não pode devolver a conexão ao pool");

                return Result.success(true);
            });

            assertEquals(0, ds.getActiveConnections(), "no fim a conexão tem de voltar ao pool");
            assertEquals(1, countFromAnotherConnection(ds));
        } finally {
            ds.close();
        }
    }

    /**
     * Construir um repositório dentro de uma transação dispara {@code introspect()}
     * ({@code begin()} + {@code execute(Function)}, que fecha no {@code finally}) — não pode encerrar a
     * transação em curso. Cenário real: {@code UserCategoryJDBCRepository} é criado lazy dentro do
     * listener de seed, já dentro da transação de criação do usuário.
     */
    @Test
    void repositoryIntrospectionInsideTransactionDoesNotCloseIt() {
        var ds = dataSource("txpropintrospect");
        Context.set(DataSource.class, () -> ds);
        try {
            ds.transaction(_ -> {
                ds.execute("INSERT INTO T VALUES (1)");

                var repository = new TRepository(); // introspect() acontece aqui
                assertFalse(repository.columns().isEmpty());
                assertEquals(1, ds.getActiveConnections(),
                        "a introspecção não pode devolver a conexão da transação em curso");

                assertEquals(0, countFromAnotherConnection(ds),
                        "a introspecção não pode ter commitado a transação em curso");
                return Result.success(true);
            });

            assertEquals(1, countFromAnotherConnection(ds));
        } finally {
            Context.remove(DataSource.class);
            ds.close();
        }
    }

    /**
     * {@code markRollbackOnly()} envenena a transação: o {@code commit()} do nível mais externo reverte.
     * Testado direto no {@link JDBCTransaction} porque o caminho equivalente pelo {@code DataSource}
     * passa por {@code Result.get()} sobre uma falha, que é fatal e encerra a JVM.
     */
    @Test
    void rollbackOnlyMakesTheOutermostCommitRevert() {
        var ds = dataSource("txproppoison");
        try {
            var transaction = switch (ds.begin()) {
                case Result.Success(var tx) -> {
                    assertNotNull(tx);
                    yield tx;
                }
                case Result.Failure(var error) -> throw new AssertionError("begin falhou: " + error);
            };

            transaction.execute("INSERT INTO T VALUES (1)").get();
            transaction.markRollbackOnly();
            transaction.commit();
            transaction.close();

            assertEquals(0, count(ds), "commit de transação envenenada tem de reverter");
            assertEquals(0, ds.getActiveConnections());
        } finally {
            ds.close();
        }
    }

    /** Repositório mínimo sobre a tabela T — só existe para exercitar o {@code introspect()}. */
    private static final class TRepository extends JDBCRepository<Long> {

        TRepository() {
            super("T");
        }

        @Override
        protected Long map(JDBCResultSet rs) {
            return rs.getLong("ID").get();
        }

        @Override
        protected Map<String, @Nullable Object> values(Long entity) {
            var values = new LinkedHashMap<String, @Nullable Object>();
            values.put("ID", entity);
            return values;
        }
    }
}
