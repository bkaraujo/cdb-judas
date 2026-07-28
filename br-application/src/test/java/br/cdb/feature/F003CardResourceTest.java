package br.cdb.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class F003CardResourceTest extends BaseHttpTest {

    private String createAccount(String name) {
        String json = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"color\":\"#007AFF\",\"active\":true}".formatted(name);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private String createCard(String accountId, String last4) {
        return asTestUser()
                .body("{\"last4\":\"%s\"}".formatted(last4))
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    @Test
    void deveGerenciarCartoesDaConta() {
        String checkingJson = """
            {"name":"Conta Corrente","type":"CHECKING","color":"#820AD1","active":true}
            """;
        String accountId = asTestUser()
                .body(checkingJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String cardId = asTestUser()
                .body("{\"last4\":\"1234\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .body("last4", is("1234"))
                .body("accountId", is(accountId))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(cardId));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(200)
                .body("cards.size()", is(1))
                .body("cards[0].last4", is("1234"));

        // Mesmo last4 na mesma conta → conflito.
        asTestUser()
                .body("{\"last4\":\"1234\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(409);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards/" + cardId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void deveBloquearExclusaoDeCartaoComTransacaoVinculadaEPermitirInativar() {
        String checkingJson = """
            {"name":"Conta Corrente","type":"CHECKING","color":"#820AD1","active":true}
            """;
        String accountId = asTestUser()
                .body(checkingJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String cardId = asTestUser()
                .body("{\"last4\":\"1234\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String macroId = asTestUser()
                .body("{\"name\":\"Moradia2\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        String categoryId = asTestUser()
                .body("{\"name\":\"Cartao\",\"nature\":\"EXPENSE\",\"parentId\":\"%s\"}".formatted(macroId))
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        String txJson = """
            {"description":"Compra","amount":-50.00,"date":"2024-04-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense","installments":1,"editMode":"single","cardId":"%s"}
            """.formatted(categoryId, cardId);
        asTestUser()
                .body(txJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards/" + cardId)
                .then().statusCode(409)
                .body("code", is("LINKED_TRANSACTIONS"))
                .body("count", is(1));

        asTestUser()
                .body("{\"active\":false}")
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards/" + cardId)
                .then().statusCode(200)
                .body("active", is(false));
    }

    @Test
    void cartaoComStrategyNaoSuportadaEhRejeitadoComo422() {
        String accountId = createAccount("Conta");
        String cardId = createCard(accountId, "1234");

        asTestUser()
                .queryParam("strategy", "DETACH")
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards/" + cardId)
                .then().statusCode(422);
    }
}
