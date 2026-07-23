package br.cdb.feature;

import br.cdb.feature.f004._1_application.UserCategoryService;
import br.cdb.feature.f000._1_application.UserService;
import br.commons.Result;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class F010UserServiceTest extends BaseHttpTest {
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
}