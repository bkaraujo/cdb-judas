package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class TagResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarTags() {
        String createJson = """
            {
              "name": "Viagem",
              "color": "#00FF00"
            }
            """;

        String tagId = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(201)
                .body("id", org.hamcrest.Matchers.notNullValue())
                .body("name", is("Viagem"))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Viagem"));

        String patchJson = """
            {
              "name": "Lazer",
              "color": "#0000FF"
            }
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(200)
                .body("name", is("Lazer"))
                .body("color", is("#0000FF"));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(200)
                .body("size()", is(0));
    }
}
