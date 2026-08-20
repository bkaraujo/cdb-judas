package br.cdb.feature.f010;

import br.cdb.BaseHttpTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class F010ImportRuleResourceTest extends BaseHttpTest {

    private String createRule(String name, String trigger) {
        String json = "{\"name\":\"%s\",\"triggers\":[\"%s\"]}".formatted(name, trigger);
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
              "name": "Companhia de Saneamento",
              "triggers": ["Companhia de Saneamento"]
            }
            """;

        String ruleId = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Companhia de Saneamento"))
                .body("triggers.size()", is(1))
                .body("triggers[0]", is("Companhia de Saneamento"))
                .body("accountId", is((Object) null))
                .body("categoryId", is((Object) null))
                .body("costCenterId", is((Object) null))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Companhia de Saneamento"))
                .body("[0].triggers.size()", is(1));

        String patchJson = """
            {
              "name": "CAESB",
              "triggers": ["CAESB", "Saneamento"]
            }
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/transaction/rules/" + ruleId)
                .then().statusCode(200)
                .body("name", is("CAESB"))
                .body("triggers.size()", is(2));

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
                .body("{\"name\":\"ab\",\"triggers\":[\"ab\"]}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(422);
    }

    @Test
    void triggersVazioEhRejeitado() {
        asTestUser()
                .body("{\"name\":\"Companhia\",\"triggers\":[]}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(422);
    }

    @Test
    void gatilhoAmbiguoNaCriacaoEhRejeitadoComConflito() {
        createRule("Companhia de Saneamento", "Companhia de Saneamento");

        asTestUser()
                .body("{\"name\":\"Companhia (rótulo)\",\"triggers\":[\"Companhia\"]}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transaction/rules")
                .then().statusCode(409);
    }

    @Test
    void gatilhoAmbiguoNaEdicaoEhRejeitadoComConflito() {
        createRule("Companhia de Saneamento", "Companhia de Saneamento");
        String outraId = createRule("Netflix", "Netflix");

        asTestUser()
                .body("{\"name\":\"Companhia (rótulo)\",\"triggers\":[\"Companhia\"]}")
                .when().patch("/api/" + TEST_USER_ID + "/accounts/transaction/rules/" + outraId)
                .then().statusCode(409);
    }
}
