package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class AccountResourceTest extends BaseHttpTest {

    @Test
    void deveCriarEGerenciarContas() {
        String createJson = """
            {
              "name": "Conta Corrente",
              "balance": 1250.50,
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true,
              "linkedAccountId": null
            }
            """;

        String id = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Conta Corrente"))
                .body("balance", is(1250.50f))
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
              "balance": 1300.00,
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true
            }
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + id)
                .then().statusCode(200)
                .body("name", is("Conta Alterada"))
                .body("balance", is(1300.00f));

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
            {"name":"X","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
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
    void deveGerenciarCartaoViaContas() {
        // Conta vinculada (CHECKING) — cartão exige mesma cor da conta de origem.
        String checkingJson = """
            {"name":"Conta Corrente","balance":0.00,"type":"CHECKING","color":"#820AD1","active":true}
            """;
        String checkingId = asTestUser()
                .body(checkingJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String cardJson = """
            {
              "name":"Nubank","balance":0.00,"type":"CREDIT_CARD","color":"#820AD1","active":true,
              "linkedAccountId":"%s",
              "last4":"1234","creditLimit":5000.00,"closingDay":5,"dueDay":12
            }
            """.formatted(checkingId);
        String cardId = asTestUser()
                .body(cardJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("type", is("CREDIT_CARD"))
                .body("linkedAccountId", is(checkingId))
                .body("last4", is("1234"))
                .body("creditLimit", is(5000.00f))
                .extract().jsonPath().getString("id");

        // Listar apenas cartões via filtro de tipo.
        asTestUser()
                .queryParam("type", "card")
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(cardId))
                .body("[0].type", is("CREDIT_CARD"));

        // Editar o cartão pelos endpoints de item de conta.
        String patchCard = """
            {
              "name":"Nubank","balance":0.00,"type":"CREDIT_CARD","color":"#820AD1","active":true,
              "linkedAccountId":"%s",
              "last4":"9999","creditLimit":8000.00,"closingDay":5,"dueDay":12
            }
            """.formatted(checkingId);
        asTestUser()
                .body(patchCard)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + cardId)
                .then().statusCode(200)
                .body("last4", is("9999"))
                .body("creditLimit", is(8000.00f));

        // Excluir o cartão.
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + cardId)
                .then().statusCode(204);

        asTestUser()
                .queryParam("type", "card")
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void saldoAtualRefleteTransacoesEnquantoSaldoInicialPermanece() {
        // Conta com saldo inicial 1000 — sem transações, saldo atual = saldo inicial.
        String accJson = """
            {"name":"Conta Corrente","balance":1000.00,"type":"CHECKING","color":"#007AFF","active":true}
            """;
        String accountId = asTestUser()
                .body(accJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .body("balance", is(1000.00f))
                .body("currentBalance", is(1000.00f))
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

        // Saldo inicial permanece 1000; saldo atual = 1000 - 250 = 750 (no item e na lista).
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(200)
                .body("balance", is(1000.00f))
                .body("currentBalance", is(750.00f));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200)
                .body("[0].balance", is(1000.00f))
                .body("[0].currentBalance", is(750.00f));
    }
}
