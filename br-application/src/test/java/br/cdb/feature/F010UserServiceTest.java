package br.cdb.feature;

import br.cdb.feature.f004._1_application.UserCategoryService;
import br.cdb.feature.f000._1_application.UserService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class F010UserServiceTest extends BaseHttpTest {
    @Inject
    UserService userService;
    @Inject
    UserCategoryService userCategoryService;

    @Test
    void reinvocarCreateUserNaoDuplicaCategoriasPadrao() {
        userService.createUser(TEST_USERNAME, "", "x".toCharArray());
        int afterFirst = userCategoryService.findAll(UUID.fromString(TEST_USER_ID)).size();
        assertTrue(afterFirst > 0);

        userService.createUser(TEST_USERNAME, "", "x".toCharArray()); // simulates a restart
        assertEquals(afterFirst, userCategoryService.findAll(UUID.fromString(TEST_USER_ID)).size(),
                "reseeding must not duplicate categories");
    }
}