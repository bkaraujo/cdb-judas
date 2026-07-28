package br.cdb.feature;

import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.test.junit.QuarkusTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class F004TagResourceTest extends BaseHttpTest {

    private static final String COST_CENTER_ID = "d0000000-0000-0000-0000-000000000002";

    private String createTag(String name) {
        String json = "{\"name\":\"%s\",\"color\":\"#00FF00\"}".formatted(name);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private String createAccount() {
        String json = """
            {"name":"Conta","type":"CHECKING","color":"#007AFF","active":true}
            """;
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private String categoryId;

    // Memoizado: chamadas repetidas na mesma transação de teste (ex.: duas transações no mesmo
    // teste) reusam a mesma categoria em vez de tentar recriar "Casa"/"Aluguel", o que a validação
    // de nome único por nível agora rejeita com 400.
    private String createCategory() {
        if (categoryId != null) return categoryId;

        String macroId = asTestUser()
                .body("{\"name\":\"Casa\",\"nature\":\"EXPENSE\"}")
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        categoryId = asTestUser()
                .body("{\"name\":\"Aluguel\",\"nature\":\"EXPENSE\",\"parentId\":\"%s\"}".formatted(macroId))
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return categoryId;
    }

    private String createTransaction(String accountId) {
        String json = """
            {"description":"Compra","amount":-50.00,"date":"2024-04-01","categoryId":"%s","costCenterId":"%s","status":"confirmed","type":"expense","installments":1,"editMode":"single"}
            """.formatted(createCategory(), COST_CENTER_ID);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private void linkTag(String transactionId, String tagId) {
        dataSource.execute(
                "INSERT INTO PERSON_TRANSACTION_TAG (COD_TRANSACTION, COD_PERSON, COD_TAG) VALUES (?, ?, ?)",
                JDBCParameter.of(transactionId, TEST_USER_ID, tagId)
        );
    }

    private long linkCount(String tagId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM PERSON_TRANSACTION_TAG WHERE COD_TAG = ?",
                JDBCParameter.of(tagId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

    private boolean transactionExists(String transactionId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM MON_TRANSACTION WHERE ID = ?",
                JDBCParameter.of(transactionId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        ) > 0;
    }

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

    @Test
    void semStrategySemVinculoPermiteExclusaoDireta() {
        String tagId = createTag("SemVinculo");
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(204);
    }

    @Test
    void semStrategyComVinculoRetorna409ComContagem() {
        String tagId = createTag("Viagem");
        String accountId = createAccount();
        String txId = createTransaction(accountId);
        linkTag(txId, tagId);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(409)
                .body("code", is("LINKED_TRANSACTIONS"))
                .body("count", is(1));
    }

    @Test
    void detachRemoveApenasOVinculoTransacaoIntacta() {
        String tagId = createTag("Viagem");
        String accountId = createAccount();
        String txId = createTransaction(accountId);
        linkTag(txId, tagId);

        asTestUser()
                .queryParam("strategy", "DETACH")
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(204);

        assertEquals(0, linkCount(tagId), "vínculo removido");
        assertTrue(transactionExists(txId), "transação permanece intacta");
    }

    @Test
    void moveReatribuiVinculosDeFormaDedupeSafe() {
        String origem = createTag("Origem");
        String destino = createTag("Destino");
        String accountId = createAccount();
        String tx1 = createTransaction(accountId);
        String tx2 = createTransaction(accountId);
        linkTag(tx1, origem);
        linkTag(tx2, origem);
        linkTag(tx2, destino); // tx2 já tem o destino: mover não pode violar a PK composta

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", destino)
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + origem)
                .then().statusCode(204);

        assertEquals(0, linkCount(origem), "tag de origem sem vínculos remanescentes");
        val destinoLinks = dataSource.query(
                "SELECT COUNT(*) FROM PERSON_TRANSACTION_TAG WHERE COD_TAG = ? AND COD_TRANSACTION IN (?, ?)",
                JDBCParameter.of(destino, tx1, tx2),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
        assertEquals(2, destinoLinks, "as duas transações apontam para o destino, sem duplicar a de tx2");
    }

    @Test
    void deleteApagaTransacoesVinculadas() {
        String tagId = createTag("Viagem");
        String accountId = createAccount();
        String txId = createTransaction(accountId);
        linkTag(txId, tagId);

        asTestUser()
                .queryParam("strategy", "DELETE")
                .when().delete("/api/" + TEST_USER_ID + "/tags/" + tagId)
                .then().statusCode(204);

        assertFalse(transactionExists(txId), "transação vinculada foi excluída");
        assertEquals(0, linkCount(tagId), "vínculo removido junto");
    }
}
