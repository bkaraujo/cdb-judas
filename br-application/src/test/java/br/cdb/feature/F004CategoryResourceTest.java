package br.cdb.feature;

import br.cdb.context.monetary._0_domain.model.Transaction.Type;
import br.cdb.feature.f004._1_application.UserCategoryService;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class F004CategoryResourceTest extends BaseHttpTest {

    private static final String COST_CENTER_ID = "d0000000-0000-0000-0000-000000000002";

    @Inject
    UserCategoryService userCategoryService;

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

    private String categoryOf(String transactionId) {
        return dataSource.query(
                "SELECT COD_CATEGORY FROM PERSON_TRANSACTION WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId),
                rs -> rs.next().get() ? rs.getString("COD_CATEGORY").get() : null
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
    void deveCriarEListarCategorys() {
        String createJson = """
            {
              "name": "Alimentação",
              "nature": "EXPENSE"
            }
            """;

        String id = asTestUser()
                .body(createJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Alimentação"))
                .body("nature", is(Type.EXPENSE.name()))
                .body("active", is(true))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(id))
                .body("[0].name", is("Alimentação"));

        String updateJson = """
            {
              "name": "Supermercado"
            }
            """;

        asTestUser()
                .body(updateJson)
                .when().patch("/api/" + TEST_USER_ID + "/categories/" + id)
                .then().statusCode(200)
                .body("id", is(id))
                .body("name", is("Supermercado"));

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + id)
                .then().statusCode(204);

        // Sem vínculos: exclusão direta, sem fallback "Outros" (removido).
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void shouldRejectSubcategoryOfSubcategory() {
        String root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        String rootId = asTestUser()
                .body(root)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String sub = """
            {"name": "Aluguel", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(rootId);
        String subId = asTestUser()
                .body(sub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String subsub = """
            {"name": "IPTU", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(subId);
        asTestUser()
                .body(subsub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(400);
    }

    @Test
    void shouldRejectSubcategoryWithDifferentNature() {
        String root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        String rootId = asTestUser()
                .body(root)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        String sub = """
            {"name": "Salário", "nature": "INCOME", "parentId": "%s"}
            """.formatted(rootId);
        asTestUser()
                .body(sub)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(400);
    }

    @Test
    void shouldCreateCategoryAndSubcategory() {
        String moradiaJson = """
            {
              "name": "Moradia",
              "nature": "EXPENSE"
            }
            """;

        String moradiaId = asTestUser()
                .body(moradiaJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Moradia"))
                .body("nature", is(Type.EXPENSE.name()))
                .extract().jsonPath().getString("id");

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(1));

        String aluguelJson = """
            {
              "name": "Aluguél",
              "nature": "EXPENSE",
              "parentId": "%s"
            }
            """.formatted(moradiaId);

        asTestUser()
                .body(aluguelJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Aluguél"))
                .body("parentId", is(moradiaId))
                .body("nature", is(Type.EXPENSE.name()));

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(2));

        // Macro com filha, sem vínculos: exclusão aplica-se à subárvore inteira; sem "Outros".
        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + moradiaId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void deveBloquearExclusaoComTransacaoVinculadaERetornarContagem() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountId = createAccount();
        createTransaction(accountId, subId);

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + macroId)
                .then().statusCode(409)
                .body("code", is("LINKED_TRANSACTIONS"))
                .body("count", is(1));
    }

    @Test
    void deveMoverTransacoesParaOutraSubcategoriaDaMesmaNatureza() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String origemId = createCategory("Aluguel", "EXPENSE", macroId);
        String destinoId = createCategory("Contas", "EXPENSE", macroId);
        String accountId = createAccount();
        String txId = createTransaction(accountId, origemId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", destinoId)
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + origemId)
                .then().statusCode(204);

        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(200)
                .body("size()", is(2)); // origem removida, macro+destino permanecem

        val recategorized = categoryOf(txId);
        org.junit.jupiter.api.Assertions.assertEquals(destinoId, recategorized, "transação recategorizada para o destino");
    }

    @Test
    void moveSemTargetIdEhRejeitadoComo422() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountId = createAccount();
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + subId)
                .then().statusCode(422);
    }

    @Test
    void moveParaCategoriaMacroEhRejeitado() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountId = createAccount();
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", macroId)
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + subId)
                .then().statusCode(400);
    }

    @Test
    void moveParaNaturezaDiferenteEhRejeitado() {
        String macroExpense = createCategory("Casa", "EXPENSE", null);
        String subExpense = createCategory("Aluguel", "EXPENSE", macroExpense);
        String macroIncome = createCategory("Salário", "INCOME", null);
        String subIncome = createCategory("Bônus", "INCOME", macroIncome);
        String accountId = createAccount();
        createTransaction(accountId, subExpense);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", subIncome)
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + subExpense)
                .then().statusCode(400);
    }

    @Test
    void moveParaCategoriaDentroDaPropriaSubarvoreEhRejeitado() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountId = createAccount();
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", subId)
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + macroId)
                .then().statusCode(400);
    }

    @Test
    void moveParaCategoriaInexistenteEh404() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountId = createAccount();
        createTransaction(accountId, subId);

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", UUID.randomUUID().toString())
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + subId)
                .then().statusCode(404);
    }

    @Test
    void deleteExpandeParaPernaDeTransferenciaEmOutraConta() {
        String macroId = createCategory("Casa", "EXPENSE", null);
        String subId = createCategory("Aluguel", "EXPENSE", macroId);
        String accountA = createAccount();
        String accountB = createAccount();

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-04-01","amount":50.00}
            """.formatted(accountA, accountB);
        String outboundId = asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");

        dataSource.execute(
                "INSERT INTO PERSON_TRANSACTION (COD_PERSON, COD_ACCOUNT, COD_TRANSACTION, COD_CATEGORY, TMS_CREATE_AT, TMS_UPDATED_AT) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                JDBCParameter.of(TEST_USER_ID, accountA, outboundId, subId)
        );

        asTestUser()
                .queryParam("strategy", "DELETE")
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + subId)
                .then().statusCode(204);

        org.junit.jupiter.api.Assertions.assertFalse(transactionExists(outboundId), "perna da transferência ligada à categoria some");

        // A perna irmã (conta B) foi expandida e removida também — par de transferência nunca fica órfão.
        val siblingCount = dataSource.query(
                "SELECT COUNT(*) FROM MON_TRANSACTION WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountB),
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
        org.junit.jupiter.api.Assertions.assertEquals(0, siblingCount, "perna irmã expandida e removida junto (par nunca fica órfão)");
    }

    @Test
    void categoriaSistemaNaoPodeSerExcluida() {
        String systemId = userCategoryService.findOrCreateUncategorizedCategory(UUID.fromString(TEST_USER_ID)).id().toString();

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/categories/" + systemId)
                .then().statusCode(400);
    }

    @Test
    void deveRejeitarCriacaoDeCategoriaDuplicadaNoMesmoNivel() {
        createCategory("Casa", "EXPENSE", null);
        asTestUser().body("""
           {"name": "Casa", "nature": "EXPENSE"}
           """)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().statusCode(400); // BusinessError.BusinessRule -> 400, confirmed via DomainExceptionMapper
    }
}
