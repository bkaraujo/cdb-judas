package br.cdb.feature;

import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.commons.MessageBus;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class F006TransactionResponseResourceTest extends BaseHttpTest {

    private long overlayCount(String transactionId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM F005_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

    private String categoryIdOf(String transactionId) {
        return dataSource.query(
                "SELECT COD_CATEGORY FROM F005_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId),
                rs -> { rs.next().get(); return rs.getString(1).get(); }
        );
    }

    private long tagLinkCount(String transactionId) {
        return dataSource.query(
                "SELECT COUNT(*) FROM F004_TRANSACTION_TAG WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

    private void linkTag(String transactionId, String tagId) {
        dataSource.execute(
                "INSERT INTO F004_TRANSACTION_TAG (COD_TRANSACTION, COD_PERSON, COD_TAG) VALUES (?, ?, ?)",
                JDBCParameter.of(transactionId, TEST_USER_ID, tagId)
        );
    }

    private String createTag() {
        return asTestUser()
                .body("{\"name\":\"Tag\",\"color\":\"#00FF00\"}")
                .when().post("/api/" + TEST_USER_ID + "/tags")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }

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

        // Criar com o personId da conta vindo do caminho (sem accountId no corpo).
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

        // Vincula uma tag antes de excluir, para provar que o join some junto (fix do bug de órfãos).
        String tagId = createTag();
        linkTag(id, tagId);
        assertEquals(1, overlayCount(id), "overlay existe antes da exclusão");

        // Excluir.
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + id)
                .then().statusCode(204);

        assertEquals(0, overlayCount(id), "overlay (F005_TRANSACTION_CATEGORY) limpo na exclusão unitária");
        assertEquals(0, tagLinkCount(id), "vínculo de tag limpo na exclusão unitária");
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
        java.util.List<String> allIds = list.jsonPath().getList("id", String.class);
        java.util.List<Object> installmentNumbers = list.jsonPath().getList("installmentNumber");
        for (int i = 0; i < installmentNumbers.size(); i++) {
            if (((Number) installmentNumbers.get(i)).intValue() == 1) {
                firstId = list.jsonPath().getString("[" + i + "].id");
                break;
            }
        }

        String tagId = createTag();
        for (val txId : allIds) linkTag(txId, tagId);

        asTestUser()
                .queryParam("mode", "FUTURE")
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions/" + firstId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(0));

        for (val txId : allIds) {
            assertEquals(0, overlayCount(txId), "overlay limpo para cada parcela apagada (FUTURE)");
            assertEquals(0, tagLinkCount(txId), "vínculo de tag limpo para cada parcela apagada (FUTURE)");
        }
    }

    @Test
    void deveTransferirEntreContas() {
        UUID origem = createAccount("#010101");
        UUID destino = createAccount("#020202");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-07-01","amount":150.00}
            """.formatted(origem, destino);

        String outboundId = asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(2));

        // Saída pertence à conta de origem.
        String inboundId = asTestUser()
                .queryParam("type", "income")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + destino + "/transactions")
                .then().statusCode(200)
                .body("size()", is(1))
                .extract().jsonPath().getString("[0].id");

        asTestUser()
                .queryParam("type", "expense")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + origem + "/transactions")
                .then().statusCode(200)
                .body("size()", is(1));

        // Vincula uma tag em cada perna: excluir uma leva a outra junto, e o fix de órfãos limpa ambas.
        String tagId = createTag();
        linkTag(outboundId, tagId);
        linkTag(inboundId, tagId);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + origem + "/transactions/" + outboundId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(0));

        assertEquals(0, tagLinkCount(outboundId), "vínculo de tag da perna de saída limpo");
        assertEquals(0, tagLinkCount(inboundId), "vínculo de tag da perna de entrada limpo (par expandido)");
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

    // Editar a conta de uma perna de transferência preserva o par (mesmo groupId, nenhuma perna
    // apagada/recriada) e não deve vazar a categoria da perna editada para a perna irmã — cada
    // perna carrega a categoria de "Transferência" da sua própria natureza (despesa/receita).
    @Test
    void deveEditarContaDePernaDeTransferenciaSemAlterarCategoriaDaPernaOposta() {
        UUID origem = createAccount("#040404");
        UUID destino = createAccount("#050505");
        UUID destinoNovo = createAccount("#060606");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-08-01","amount":200.00}
            """.formatted(origem, destino);

        String outboundId = asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String inboundId = asTestUser()
                .queryParam("type", "income")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + destino + "/transactions")
                .then().statusCode(200)
                .body("size()", is(1))
                .extract().jsonPath().getString("[0].id");

        String outboundCategoryBefore = categoryIdOf(outboundId);
        String inboundCategoryBefore = categoryIdOf(inboundId);
        assertNotEquals(outboundCategoryBefore, inboundCategoryBefore,
                "pernas de transferência têm categoria por natureza (despesa/receita) — devem começar diferentes");

        UUID novaCategoria = createLeafCategory();
        assertNotEquals(novaCategoria.toString(), outboundCategoryBefore);

        String patchJson = """
            {"description":"Transferência (saída)","amount":200.00,"date":"2024-08-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense"}
            """.formatted(novaCategoria);

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + destinoNovo + "/transactions/" + outboundId)
                .then().statusCode(200);

        List<String> groupIds = asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("size()", is(2))
                .extract().jsonPath().getList("groupId", String.class);
        assertEquals(groupIds.get(0), groupIds.get(1), "par preservado — mesmo groupId nas duas pernas");

        assertEquals(novaCategoria.toString(), categoryIdOf(outboundId), "categoria da perna editada foi aplicada");
        assertEquals(inboundCategoryBefore, categoryIdOf(inboundId),
                "categoria da perna irmã não deve ser sobrescrita ao corrigir a conta da outra perna");
    }

    // updateTransfer (contexto monetário) espelha data/valor/status na perna irmã — uma mutação
    // real fora da conta da perna editada, que também precisa de refresh SSE.
    @Test
    void deveNotificarRefreshDaContaOpostaAoEditarPernaDeTransferencia() {
        UUID origem = createAccount("#070707");
        UUID destino = createAccount("#080808");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-09-01","amount":75.00}
            """.formatted(origem, destino);

        String outboundId = asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        UUID outboundCategoria = UUID.fromString(categoryIdOf(outboundId));

        List<UUID> refreshedAccountIds = new CopyOnWriteArrayList<>();
        MessageBus.subscribe(new Object() {
            @br.commons.framework.message.MessageListener
            public br.commons.framework.message.MessageResult on(AccountStreamEvents.Refresh event) {
                refreshedAccountIds.add(event.accountId());
                return br.commons.framework.message.MessageResult.AVAILABLE;
            }
        });

        String patchJson = """
            {"description":"Transferência (saída)","amount":75.00,"date":"2024-09-02","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"confirmed","type":"expense"}
            """.formatted(outboundCategoria);

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + origem + "/transactions/" + outboundId)
                .then().statusCode(200);

        assertTrue(refreshedAccountIds.contains(origem), "conta da perna editada deve receber refresh");
        assertTrue(refreshedAccountIds.contains(destino), "conta da perna irmã (espelhada) também deve receber refresh");
    }
}
