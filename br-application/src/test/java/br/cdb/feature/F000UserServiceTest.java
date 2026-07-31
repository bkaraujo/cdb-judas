package br.cdb.feature;

import br.cdb.feature.f000._1_application.service.UserService;
import br.cdb.feature.f005._1_application.UserCategoryService;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class F000UserServiceTest extends BaseHttpTest {
    @Inject
    UserService userService;
    @Inject
    UserCategoryService userCategoryService;

    /** Username próprio (não {@link #TEST_USERNAME}, já semeado direto no banco por
     *  {@code BaseHttpTest.seedUser} no {@code @BeforeEach}, sem passar por {@code UserService}/
     *  publicar {@code UserEvents.Created} — colidiria com o {@code createUser} abaixo). */
    @Test
    void reinvocarCreateUserNaoDuplicaCategoriasPadrao() {
        String username = "restart-user";
        // Categorias são chaveadas pela Person (COD_PERSON), não pelo login — busca por personId.
        UUID personId = switch (userService.createUser(username, "", "x".toCharArray())) {
            case Result.Success(var user) -> UUID.fromString(Objects.requireNonNull(user.personId()));
            case Result.Failure(var error) -> throw new IllegalStateException(error.toString());
        };

        int afterFirst = userCategoryService.findAll(personId).size();
        assertTrue(afterFirst > 0);

        userService.createUser(username, "", "x".toCharArray()); // simulates a restart
        assertEquals(afterFirst, userCategoryService.findAll(personId).size(),
                "reseeding must not duplicate categories");
    }

    /**
     * Formato do {@code f999}: {@code createUser} envolvido por {@code dataSource.transaction(...)} tem
     * de ser <b>uma única transação</b> — Person, User, credencial e o seed de categorias de {@code f005}
     * (disparado sincronamente por {@code UserEvents.Created}) commitam juntos, na mesma conexão.
     */
    @Test
    void criacaoDeUsuarioCabeNumaUnicaTransacao() {
        String username = "atomic-user";
        int baseline = dataSource.getActiveConnections();

        var user = dataSource.transaction(_ -> {
            var created = switch (userService.createUser(username, "", "x".toCharArray())) {
                case Result.Success(var u) -> u;
                case Result.Failure(var error) -> throw new IllegalStateException(error.toString());
            };

            assertEquals(baseline + 1, dataSource.getActiveConnections(),
                    "toda a criação tem de correr na conexão da transação externa");
            assertEquals(0, countUsers(username),
                    "nada pode estar commitado antes do fim da transação externa");

            return Result.success(created);
        });

        UUID personId = UUID.fromString(Objects.requireNonNull(user.personId()));
        assertEquals(1, countUsers(username), "o commit externo tem de publicar o login criado");
        assertTrue(userCategoryService.findAll(personId).size() > 0,
                "as categorias semeadas pelo evento têm de vir no mesmo commit");
        assertEquals(baseline, dataSource.getActiveConnections(), "a conexão tem de voltar ao pool");
    }

    /** Conta logins a partir de outra thread — logo, de outra conexão: só enxerga o que já commitou. */
    private long countUsers(String username) {
        var seen = new AtomicLong(-1);
        var thread = Thread.ofPlatform().start(() -> seen.set(dataSource.query(
                "SELECT COUNT(*) FROM F000_USER WHERE TXT_USERNAME = ?",
                JDBCParameter.of(username),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        )));

        try {
            thread.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("leitura noutra conexão interrompida");
        }
        assertFalse(thread.isAlive(), "a leitura noutra conexão não devia bloquear");
        return seen.get();
    }
}