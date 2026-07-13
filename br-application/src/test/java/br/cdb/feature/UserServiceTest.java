package br.cdb.feature;

import br.cdb.feature.system.user.UserService;
import br.cdb.feature.user.categories.UserCategoryService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserServiceTest extends BaseHttpTest {
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