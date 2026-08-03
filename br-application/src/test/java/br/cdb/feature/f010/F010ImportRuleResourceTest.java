package br.cdb.feature.f010;

import br.cdb.BaseHttpTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class F010ImportRuleResourceTest extends BaseHttpTest {

    private String createRule(String name) {
        String json = "{\"name\":\"%s\"}".formatted(name);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    @Test
    void deveGerenciarRegras() {
        String createJson = """
            {
              "name": "Companhia de Saneamento"
            }
            """;

        String ruleId = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Companhia de Saneamento"))
                .body("accountId", is((Object) null))
                .body("categoryId", is((Object) null))
                .body("costCenterId", is((Object) null))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Companhia de Saneamento"));

        String patchJson = """
            {
              "name": "CAESB"
            }
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/transaction/rules/" + ruleId)
                .then().statusCode(200)
                .body("name", is("CAESB"));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/transaction/rules/" + ruleId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void nomeCurtoDemaisEhRejeitado() {
        asTestUser()
                .body("{\"name\":\"ab\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(422);
    }

    @Test
    void padraoAmbiguoNaCriacaoEhRejeitadoComConflito() {
        createRule("Companhia de Saneamento");

        asTestUser()
                .body("{\"name\":\"Companhia\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(409);
    }

    @Test
    void padraoAmbiguoNaEdicaoEhRejeitadoComConflito() {
        createRule("Companhia de Saneamento");
        String outraId = createRule("Netflix");

        asTestUser()
                .body("{\"name\":\"Companhia\"}")
                .when().patch("/api/" + TEST_USER_ID + "/accounts/transaction/rules/" + outraId)
                .then().statusCode(409);
    }
}
