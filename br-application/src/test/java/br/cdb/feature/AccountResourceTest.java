package br.cdb.feature;

import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.test.junit.QuarkusTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AccountResourceTest extends BaseHttpTest {

    private static final String COST_CENTER_ID = "d0000000-0000-0000-0000-000000000002";

    private String createAccount(String name) {
        String json = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"color\":\"#007AFF\",\"active\":true}".formatted(name);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private String createCategory(String name, String nature, String parentId) {
        String json = parentId == null
                ? "{\"name\":\"%s\",\"nature\":\"%s\"}".formatted(name, nature)
                : "{\"name\":\"%s\",\"nature\":\"%s\",\"parentId\":\"%s\"}".formatted(name, nature, parentId);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

    private String createTransaction(String accountId, String categoryId) {
        String json = """
            {"description":"Compra","amount":-50.00,"date":"2024-04-01","categoryId":"%s","costCenterId":"%s","status":"confirmed","type":"expense","installments":1,"editMode":"single"}
            """.formatted(categoryId, COST_CENTER_ID);
        return asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
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

    private boolean transactionExists(String transactionId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM MON_TRANSACTION WHERE ID = ?",
                JDBCParameter.of(transactionId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        ) > 0;
    }

    private long balanceRowCount(String accountId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM USER_ACCOUNT_BALANCE WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

    private long overlayRowCount(String accountId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM USER_TRANSACTION WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

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

    @Test
    void contaSemTransacoesEhExcluidaDiretamente() {
        String accountId = createAccount("Conta vazia");
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(204);
    }

    @Test
    void contaComTransacoesSemStrategyRetorna409ComContagem() {
        String accountId = createAccount("Conta");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createTransaction(accountId, subId);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(409)
                .body("code", is("LINKED_TRANSACTIONS"))
                .body("count", is(1));
    }

    @Test
    void moveMigraTransacaoPreservandoCategoriaECartoesMovemSaldoOrigemSome() {
        String sourceId = createAccount("Origem");
        String targetId = createAccount("Destino");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createCard(sourceId, "1234");
        String txId = createTransaction(sourceId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", targetId)
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + sourceId)
                .then().statusCode(204);

        val row = dataSource.query(
                "SELECT COD_ACCOUNT, COD_CATEGORY FROM USER_TRANSACTION WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(txId),
                rs -> {
                    rs.next().get();
                    return new String[]{rs.getString("COD_ACCOUNT").get(), rs.getString("COD_CATEGORY").get()};
                }
        );
        assertEquals(targetId, row[0], "overlay re-keyed para a conta destino");
        assertEquals(subId, row[1], "categoria preservada pelo re-key");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + targetId)
                .then().statusCode(200)
                .body("cards.size()", is(1));

        assertEquals(0, balanceRowCount(sourceId), "saldos da origem apagados");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + sourceId)
                .then().statusCode(404);
    }

    @Test
    void moveParaSiMesmaEhRejeitada() {
        String accountId = createAccount("Conta");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", accountId)
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(400);
    }

    @Test
    void moveParaContaInativaEhRejeitada() {
        String sourceId = createAccount("Origem");
        String inactiveTarget = asTestUser()
                .body("{\"name\":\"Inativa\",\"type\":\"CHECKING\",\"color\":\"#007AFF\",\"active\":false}")
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createTransaction(sourceId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", inactiveTarget)
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + sourceId)
                .then().statusCode(400);
    }

    @Test
    void moveParaContaInexistenteEh404() {
        String accountId = createAccount("Conta");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", UUID.randomUUID().toString())
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(404);
    }

    @Test
    void moveSemTargetIdEhRejeitadoComo422() {
        String accountId = createAccount("Conta");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId)
                .then().statusCode(422);
    }

    @Test
    void deleteApagaTransacoesEOverlayMasPernaDeTransferenciaEmOutraContaSobrevive() {
        String accountA = createAccount("Conta A");
        String accountB = createAccount("Conta B");
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String regularTxId = createTransaction(accountA, subId);

        String tagId = asTestUser()
                .body("{\"name\":\"Tag\",\"color\":\"#00FF00\"}")
                .when().post("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        dataSource.execute(
                "INSERT INTO USER_TRANSACTION_TAG (COD_TRANSACTION, COD_USER, COD_TAG) VALUES (?, ?, ?)",
                JDBCParameter.of(regularTxId, TEST_USER_ID, tagId)
        );

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-04-01","amount":50.00}
            """.formatted(accountA, accountB);
        String outboundId = asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        asTestUser()
                .queryParam("strategy", "DELETE")
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountA)
                .then().statusCode(204);

        assertFalse(transactionExists(regularTxId), "transação comum da conta A some");
        assertFalse(transactionExists(outboundId), "perna de transferência da conta A some");
        assertEquals(0, overlayRowCount(accountA), "overlay da conta A limpo");

        val tagLinkCount = dataSource.query(
                "SELECT COUNT(*) FROM USER_TRANSACTION_TAG WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(regularTxId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
        assertEquals(0, tagLinkCount, "vínculo de tag da transação apagada também é limpo");

        val accountBTxCount = dataSource.query(
                "SELECT COUNT(*) FROM MON_TRANSACTION WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountB),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
        assertTrue(accountBTxCount > 0, "perna irmã na conta B sobrevive (Purge de conta não expande transferência)");
    }
}
