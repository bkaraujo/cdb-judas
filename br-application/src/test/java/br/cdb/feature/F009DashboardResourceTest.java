package br.cdb.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class F009DashboardResourceTest extends BaseHttpTest {

    private static final String COST_CENTER_ID = "d0000000-0000-0000-0000-000000000002";

    @Test
    void deveObterResultadoMensal() {
        asTestUser()
                .queryParam("month", "3")
                .queryParam("year", "2024")
                .when().get("/api/" + TEST_USER_ID + "/dashboard")
                .then().statusCode(200)
                .body("incomes", notNullValue())
                .body("expenses", notNullValue())
                .body("result", notNullValue())
                .body("history", instanceOf(java.util.List.class));
    }

    /** Exercita o caminho novo (leitura via InternalApi contra o endpoint público de f006, fase 6 de
     *  .claude/plan.md) de ponta a ponta: só transação confirmada dentro do mês entra na soma. */
    @Test
    void deveSomarSoTransacoesConfirmadasDoMes() {
        UUID accountId = createAccount();
        UUID incomeCategoryId = createLeafCategory("income");
        UUID expenseCategoryId = createLeafCategory("expense");

        createTransaction(accountId, incomeCategoryId, "Salário", "3000.00", "2024-05-05", "confirmed", "income");
        createTransaction(accountId, expenseCategoryId, "Mercado", "-400.00", "2024-05-10", "confirmed", "expense");
        createTransaction(accountId, expenseCategoryId, "Fora do mês", "-999.00", "2024-04-30", "confirmed", "expense");
        createTransaction(accountId, expenseCategoryId, "Ainda pendente", "-111.00", "2024-05-15", "pending", "expense");

        asTestUser()
                .queryParam("month", "5")
                .queryParam("year", "2024")
                .when().get("/api/" + TEST_USER_ID + "/dashboard")
                .then().statusCode(200)
                .body("incomes", is(3000.00f))
                .body("expenses", is(400.00f))
                .body("result", is(2600.00f));
    }

    private UUID createAccount() {
        String id = asTestUser()
                .body("""
                    {"name":"Conta","balance":0,"type":"CHECKING","color":"#000000","active":true}
                    """)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }

    // Transações só podem ser lançadas em subcategorias (não em macro-categorias).
    private UUID createLeafCategory(String nature) {
        String macroJson = """
            {"name":"Macro %s","nature":"%s"}
            """.formatted(nature, nature);
        String macroId = asTestUser()
                .body(macroJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");

        String subJson = """
            {"name":"Sub %s","nature":"%s","parentId":"%s"}
            """.formatted(nature, nature, macroId);
        String subId = asTestUser()
                .body(subJson)
                .when().post("/api/" + TEST_USER_ID + "/categories")
                .then().extract().jsonPath().getString("id");
        return UUID.fromString(subId);
    }

    private void createTransaction(UUID accountId, UUID categoryId, String description, String amount, String date, String status, String type) {
        String json = """
            {
              "description": "%s",
              "amount": %s,
              "date": "%s",
              "categoryId": "%s",
              "costCenterId": "%s",
              "status": "%s",
              "type": "%s",
              "installments": 1,
              "editMode": "single"
            }
            """.formatted(description, amount, date, categoryId, COST_CENTER_ID, status, type);
        asTestUser()
                .body(json)
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/transactions")
                .then().statusCode(201);
    }
}
