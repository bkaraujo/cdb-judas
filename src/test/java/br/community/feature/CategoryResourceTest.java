package br.community.feature;

import br.community.context.monetary._0_domain.model.Transaction.Type;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class CategoryResourceTest extends BaseHttpTest {

    @Test
    void deveCriarEListarCategorys() {
        String createJson = """
            {
              "name": "Alimentação",
              "nature": "EXPENSE"
            }
            """;

        String id = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Alimentação"))
                .body("nature", is(Type.EXPENSE.name()))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(id))
                .body("[0].name", is("Alimentação"));

        String updateJson = """
            {
              "name": "Supermercado"
            }
            """;

        asTestUser()
                .body(updateJson)
                .when().patch("/api/" + TEST_USER_ID + "/categories/" + id)
                .then().statusCode(200)
                .body("id", is(id))
                .body("name", is("Supermercado"));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + id)
                .then().statusCode(204);

        // After deletion, "Outros" fallback category is auto-created
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Outros"));
    }

    @Test
    void shouldRejectSubcategoryOfSubcategory() {
        String root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        String rootId = asTestUser()
                .body(root)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String sub = """
            {"name": "Aluguel", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(rootId);
        String subId = asTestUser()
                .body(sub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String subsub = """
            {"name": "IPTU", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(subId);
        asTestUser()
                .body(subsub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(400);
    }

    @Test
    void shouldRejectSubcategoryWithDifferentNature() {
        String root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        String rootId = asTestUser()
                .body(root)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String sub = """
            {"name": "Salário", "nature": "INCOME", "parentId": "%s"}
            """.formatted(rootId);
        asTestUser()
                .body(sub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(400);
    }

    @Test
    void shouldCreateCategoryAndSubcategory() {
        String moradiaJson = """
            {
              "name": "Moradia",
              "nature": "EXPENSE"
            }
            """;

        String moradiaId = asTestUser()
                .body(moradiaJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Moradia"))
                .body("nature", is(Type.EXPENSE.name()))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1));

        String aluguelJson = """
            {
              "name": "Aluguél",
              "nature": "EXPENSE",
              "parentId": "%s"
            }
            """.formatted(moradiaId);

        asTestUser()
                .body(aluguelJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Aluguél"))
                .body("parentId", is(moradiaId))
                .body("nature", is(Type.EXPENSE.name()));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(2));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + moradiaId)
                .then().statusCode(204);

        // After deletion, "Outros" fallback category is auto-created
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Outros"));
    }
}
