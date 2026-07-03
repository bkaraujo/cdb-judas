package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
public class AccountResourceTest extends BaseHttpTest {

    @Test
    void deveCriarEGerenciarContas() {
        String createJson = """
            {
              "name": "Conta Corrente",
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true
            }
            """;

        String id = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Conta Corrente"))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + id)
                .then().statusCode(200)
                .body("id", is(id));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200)
                .body("size()", is(1));

        String patchJson = """
            {
              "name": "Conta Alterada",
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true
            }
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + id)
                .then().statusCode(200)
                .body("name", is("Conta Alterada"));

        asTestUser()
                .queryParam("year", "2024")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + id + "/balance")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + id + "/balance")
                .then().statusCode(422);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + id)
                .then().statusCode(204);
    }

    @Test
    void deveRetornar404ParaContaInexistente() {
        UUID missing = UUID.randomUUID();

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + missing)
                .then().statusCode(404);

        String patchJson = """
            {"name":"X","type":"CHECKING","color":"#000000","active":true}
            """;
        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + missing)
                .then().statusCode(404);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + missing)
                .then().statusCode(404);
    }

    @Test
    void deveGerenciarLimiteECicloDeFaturaDaConta() {
        String createJson = """
            {
              "name":"Conta Corrente","type":"CHECKING","color":"#820AD1","active":true,
              "creditLimit":5000.00,"overdraftLimit":500.00,"closingDay":5,"dueDay":12
            }
            """;
        String accountId = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("creditLimit", is(5000.00f))
                .body("overdraftLimit", is(500.00f))
                .body("closingDay", is(5))
                .body("dueDay", is(12))
                .body("cards.size()", is(0))
                .extract().jsonPath().getString("id");

        String patchJson = """
            {
              "name":"Conta Corrente","type":"CHECKING","color":"#820AD1","active":true,
              "creditLimit":8000.00,"closingDay":10,"dueDay":20
            }
            """;
        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(200)
                .body("creditLimit", is(8000.00f))
                .body("overdraftLimit", nullValue())
                .body("closingDay", is(10))
                .body("dueDay", is(20));
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
    void deveRejeitarTipoDeContaDesconhecido() {
        String cardJson = """
            {"name":"Nubank","type":"CREDIT_CARD","color":"#820AD1","active":true}
            """;
        asTestUser()
                .body(cardJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(422);
    }

    @Test
    void saldoAtualEhSomaPuraDasTransacoes() {
        // Conta nova — sem conceito de saldo inicial; sem transações, saldo atual = 0.
        String accJson = """
            {"name":"Conta Corrente","type":"CHECKING","color":"#007AFF","active":true}
            """;
        String accountId = asTestUser()
                .body(accJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("currentBalance", is(0.0f))
                .extract().jsonPath().getString("id");

        // Transações só podem ser lançadas em subcategorias (folhas).
        String macroId = asTestUser()
                .body("{\"name\":\"Moradia\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        String categoryId = asTestUser()
                .body("{\"name\":\"Aluguel\",\"nature\":\"EXPENSE\",\"parentId\":\"%s\"}".formatted(macroId))
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        // Lançamento de -250 na conta.
        String txJson = """
            {"description":"Aluguel","amount":-250.00,"date":"2024-04-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense","installments":1,"editMode":"single"}
            """.formatted(categoryId);
        asTestUser()
                .body(txJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201);

        // Saldo atual = soma pura das transações = -250 (no item e na lista).
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(200)
                .body("currentBalance", is(-250.00f));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200)
                .body("[0].currentBalance", is(-250.00f));
    }
}
