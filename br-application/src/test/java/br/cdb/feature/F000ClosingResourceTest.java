package br.cdb.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
public class F000ClosingResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarFechamento() {
        asTestUser()
                .body("{\"period\":\"2024-02\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200)
                .body("period", is("2024-02"));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200)
                .body("period", is("2024-02"));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200)
                .body("period", nullValue());
    }

    @Test
    void deveBloquearLancamentoEmPeriodoFechado() {
        UUID accountId = UUID.fromString(asTestUser()
                .body("{\"name\":\"Conta F\",\"value\":0.00,\"type\":\"CHECKING\",\"color\":\"#000000\",\"active\":true}")
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id"));

        String macroId = asTestUser()
                .body("{\"name\":\"Geral\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        String categoryId = asTestUser()
                .body("{\"name\":\"Sub\",\"nature\":\"EXPENSE\",\"parentId\":\"" + macroId + "\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        asTestUser()
                .body("{\"period\":\"2024-06\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200);

        String blockedJson = """
            {"description":"Dentro do fechado","amount":-50.00,"date":"2024-06-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        asTestUser()
                .body(blockedJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(400);

        String okJson = """
            {"description":"Pos fechamento","amount":-50.00,"date":"2024-07-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        asTestUser()
                .body(okJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201);
    }

    @Test
    void deveBloquearEdicaoEExclusaoEmPeriodoFechado() {
        UUID accountId = UUID.fromString(asTestUser()
                .body("{\"name\":\"Conta E\",\"value\":0.00,\"type\":\"CHECKING\",\"color\":\"#000000\",\"active\":true}")
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id"));

        String macroId = asTestUser()
                .body("{\"name\":\"Geral\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        String categoryId = asTestUser()
                .body("{\"name\":\"Sub\",\"nature\":\"EXPENSE\",\"parentId\":\"" + macroId + "\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        // lançamento criado ANTES de fechar o período
        String txJson = """
            {"description":"Antes","amount":-50.00,"date":"2024-06-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        String txId = asTestUser()
                .body(txJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        // fecha o período que cobre o lançamento
        asTestUser()
                .body("{\"period\":\"2024-06\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200);

        String updJson = """
            {"description":"Tentativa","amount":-60.00,"date":"2024-06-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        asTestUser()
                .body(updJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + txId)
                .then().statusCode(400);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + txId)
                .then().statusCode(400);
    }

    @Test
    void deveBloquearTransferenciaEmPeriodoFechado() {
        String from = asTestUser()
                .body("{\"name\":\"Origem\",\"value\":100.00,\"type\":\"CHECKING\",\"color\":\"#000000\",\"active\":true}")
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id");
        String to = asTestUser()
                .body("{\"name\":\"Destino\",\"value\":0.00,\"type\":\"CHECKING\",\"color\":\"#000000\",\"active\":true}")
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id");

        asTestUser()
                .body("{\"period\":\"2024-06\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/closing")
                .then().statusCode(200);

        String transfer = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-06-10","amount":30.00}
            """.formatted(from, to);
        asTestUser()
                .body(transfer)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(400);
    }
}
