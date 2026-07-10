package br.community.feature;

import br.commons.tools.Strings;
import br.community.feature.system.user.UserService;
import br.community.feature.user.categories.UserCategoryService;
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
        val TEST_USERNAME = Strings.EMPTY;

        userService.createUser(TEST_USERNAME, "", "x".toCharArray());
        int afterFirst = userCategoryService.findAll(UUID.fromString(TEST_USER_ID)).size();
        assertTrue(afterFirst > 0);

        userService.createUser(TEST_USERNAME, "", "x".toCharArray()); // simulates a restart
        assertEquals(afterFirst, userCategoryService.findAll(UUID.fromString(TEST_USER_ID)).size(),
                "reseeding must not duplicate categories");
    }
}