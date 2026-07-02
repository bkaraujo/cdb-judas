package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class TransactionResponseResourceTest extends BaseHttpTest {

    private UUID createAccount(String color) {
        String json = """
            {"name":"Conta","balance":1000.00,"type":"CHECKING","color":"%s","active":true}
            """.formatted(color);
        String id = asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }

    private UUID createCard(UUID accountId, String last4) {
        String id = asTestUser()
                .body("{\"last4\":\"%s\"}".formatted(last4))
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }

    // Transações só podem ser lançadas em subcategorias (não em macro-categorias).
    private UUID createLeafCategory() {
        String macroId = asTestUser()
                .body("{\"name\":\"Moradia\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        String subJson = """
            {"name":"Aluguel","nature":"EXPENSE","parentId":"%s"}
            """.formatted(macroId);
        String subId = asTestUser()
                .body(subJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        return UUID.fromString(subId);
    }

    @Test
    void deveGerenciarTransacoesPorConta() {
        UUID accountId = createAccount("#000000");
        UUID categoryId = createLeafCategory();

        // Criar com o id da conta vindo do caminho (sem accountId no corpo).
        String createJson = """
            {
              "description": "Pagamento Aluguel",
              "amount": -2500.00,
              "date": "2024-04-01",
              "categoryId": "%s",
              "costCenterId": "d0000000-0000-0000-0000-000000000002",
              "status": "pending",
              "type": "expense",
              "installments": 1,
              "editMode": "single"
            }
            """.formatted(categoryId);

        String id = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201)
                .body("id", org.hamcrest.Matchers.notNullValue())
                .body("amount", is(-2500.00f))
                .body("accountId", is(accountId.toString()))
                .extract().jsonPath().getString("id");

        // Lista entre contas com filtros.
        asTestUser()
                .queryParam("limit", "10")
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("[0].description", is("Pagamento Aluguel"));

        asTestUser()
                .queryParam("status", "pending")
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(1));
        asTestUser()
                .queryParam("status", "confirmed")
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(0));

        // Lista por conta.
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(200)
                .body("size()", is(1));

        // Alterar status informando data de pagamento.
        asTestUser()
                .body("{\"status\":\"confirmed\",\"paymentDate\":\"2024-04-02\"}")
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + id + "/status")
                .then().statusCode(200)
                .body("status", is("confirmed"))
                .body("paymentDate", is("2024-04-02"));

        // Excluir.
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + id)
                .then().statusCode(204);
    }

    @Test
    void deveCriarParcelasEExcluirFuturas() {
        UUID accountId = createAccount("#222222");
        UUID categoryId = createLeafCategory();

        String createJson = """
            {"description":"TV","amount":-300.00,"date":"2024-06-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense","installments":3,"editMode":"single"}
            """.formatted(categoryId);

        asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201)
                .body("groupId", org.hamcrest.Matchers.notNullValue())
                .body("installmentNumber", is(1))
                .body("totalInstallments", is(3));

        Response list = asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(3))
                .extract().response();

        String firstId = null;
        java.util.List<Object> installmentNumbers = list.jsonPath().getList("installmentNumber");
        for (int i = 0; i < installmentNumbers.size(); i++) {
            if (((Number) installmentNumbers.get(i)).intValue() == 1) {
                firstId = list.jsonPath().getString("[" + i + "].id");
                break;
            }
        }

        asTestUser()
                .queryParam("mode", "FUTURE")
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + firstId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void deveTransferirEntreContas() {
        UUID origem = createAccount("#010101");
        UUID destino = createAccount("#020202");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-07-01","amount":150.00}
            """.formatted(origem, destino);

        asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(2));

        // Saída pertence à conta de origem.
        asTestUser()
                .queryParam("type", "expense")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + origem + "/transactions")
                .then().statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void deveAceitarTransacaoComCartaoDaPropriaConta() {
        UUID accountId = createAccount("#101010");
        UUID cardId = createCard(accountId, "1234");
        UUID categoryId = createLeafCategory();

        String createJson = """
            {"description":"Compra","amount":-50.00,"date":"2024-05-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense","installments":1,"editMode":"single","cardId":"%s"}
            """.formatted(categoryId, cardId);

        asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201)
                .body("cardId", is(cardId.toString()));
    }

    @Test
    void deveRejeitarTransacaoComCartaoDeOutraConta() {
        UUID accountA = createAccount("#202020");
        UUID accountB = createAccount("#303030");
        UUID cardOfB = createCard(accountB, "9999");
        UUID categoryId = createLeafCategory();

        String createJson = """
            {"description":"Compra","amount":-50.00,"date":"2024-05-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense","installments":1,"editMode":"single","cardId":"%s"}
            """.formatted(categoryId, cardOfB);

        asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountA + "/transactions")
                .then().statusCode(400);
    }
}
