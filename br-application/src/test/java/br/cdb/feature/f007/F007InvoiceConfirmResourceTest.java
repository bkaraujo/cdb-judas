package br.cdb.feature.f007;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Confirmação de importação de fatura pela interface Web
 * ({@code POST /accounts/transactions/import/confirm}): o que fica gravado depois que o usuário aceita
 * as linhas do preview — roteamento de cada linha para a conta do seu cartão, status por mês,
 * agrupamento do carnê e a idempotência que torna reimportar o mesmo PDF inofensivo.
 */
@QuarkusTest
class F007InvoiceConfirmResourceTest extends AbstractImportTest {

    @Test
    void confirmRoteiaCadaLinhaParaAContaDoSeuCartao() {
        val contaA = seedAccount("Conta A");
        val contaB = seedAccount("Conta B");
        val cartaoA = seedCard(contaA, "0020");
        val cartaoB = seedCard(contaB, "9999");
        val categoryId = seedLeafCategory();

        confirm(row("Compra A", "50.00", "2025-07-03", cartaoA, categoryId),
                row("Compra B", "70.00", "2025-07-04", cartaoB, categoryId))
                .body("created", is(2))
                .body("skipped", is(0));

        val transactions = listTransactions();
        assertEquals(contaA.toString(), find(transactions, "Compra A").get("accountId"));
        assertEquals(cartaoA.toString(), find(transactions, "Compra A").get("cardId"));
        assertEquals(contaB.toString(), find(transactions, "Compra B").get("accountId"));
        assertEquals(cartaoB.toString(), find(transactions, "Compra B").get("cardId"));
        // 1:1 com a transação: cada linha importada leva a categoria escolhida na linha.
        assertTrue(transactions.stream().allMatch(t -> categoryId.toString().equals(t.get("categoryId"))));
    }

    @Test
    void confirmGravaAVistaEParceladoComStatusPorMes() {
        val accountId = seedAccount("Conta");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();

        val hoje = LocalDate.now();
        val futuro = hoje.plusMonths(2);
        confirm(row("Mercado", "80.00", hoje.toString(), cardId, categoryId),
                row("Streaming", "30.00", futuro.toString(), cardId, categoryId),
                installmentRow("Geladeira", "100.00", hoje.toString(), hoje.toString(), 1, 2, cardId, categoryId),
                installmentRow("Geladeira", "100.00", hoje.plusMonths(1).toString(), hoje.toString(), 2, 2, cardId, categoryId))
                .body("created", is(4))
                .body("skipped", is(0));

        val transactions = listTransactions();
        assertEquals(4, transactions.size());
        assertTrue(transactions.stream().allMatch(t -> accountId.toString().equals(t.get("accountId"))));
        assertTrue(transactions.stream().allMatch(t -> "expense".equals(t.get("type"))));

        assertEquals("confirmed", find(transactions, "Mercado").get("status"));
        assertEquals("scheduled", find(transactions, "Streaming").get("status"));

        val parcelas = transactions.stream().filter(t -> "Geladeira".equals(t.get("description"))).toList();
        assertEquals(2, parcelas.size());
        assertEquals(1L, parcelas.stream().map(t -> t.get("groupId")).distinct().count(), "o carnê é um grupo só");
        assertNotNull(parcelas.getFirst().get("groupId"));
        assertEquals(List.of(1, 2), parcelas.stream().map(t -> (Integer) t.get("installmentNumber")).sorted().toList());
        assertTrue(parcelas.stream().allMatch(t -> Integer.valueOf(2).equals(t.get("totalInstallments"))));
    }

    @Test
    void confirmEhIdempotenteNaReimportacaoDoMesmoDocumento() {
        val accountId = seedAccount("Conta");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();

        val rows = new String[]{
                row("Uber", "25.00", "2025-07-08", cardId, categoryId),
                installmentRow("Notebook", "200.00", "2025-07-15", "2025-07-15", 3, 4, cardId, categoryId),
                installmentRow("Notebook", "200.00", "2025-08-15", "2025-07-15", 4, 4, cardId, categoryId)};

        confirm(rows).body("created", is(3)).body("skipped", is(0));
        assertEquals(3, listTransactions().size());

        confirm(rows).body("created", is(0)).body("skipped", is(3));
        assertEquals(3, listTransactions().size(), "reimportar não pode duplicar nada");
    }

    @Test
    void confirmPulaAVistaJaLancadaAindaQueComOutraGrafia() {
        val accountId = seedAccount("Conta");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();
        // Já lançada manualmente: mesma conta, data e valor; a descrição só difere em caixa.
        createTransaction(accountId, categoryId, "MERCADO LIVRE", "-90.00", "2025-07-04");

        confirm(row("Mercado Livre", "90.00", "2025-07-04", cardId, categoryId))
                .body("created", is(0))
                .body("skipped", is(1));

        assertEquals(1, listTransactions().size());
    }

    @Test
    void confirmComCartaoDesconhecidoRetorna422() {
        val categoryId = seedLeafCategory();

        confirmRaw(row("Compra", "10.00", "2025-07-01", UUID.randomUUID(), categoryId))
                .statusCode(422)
                .body("code", is("CARD_NOT_FOUND"));
    }

    @Test
    void confirmSemCartaoNaLinhaRetorna422() {
        val categoryId = seedLeafCategory();
        val body = """
                {"description":"Compra","amount":10.00,"date":"2025-07-01","originalDate":"2025-07-01",
                 "categoryId":"%s"}
                """.formatted(categoryId);

        confirmRaw(body)
                .statusCode(422)
                .body("code", is("CARD_REQUIRED"));
    }

    @Test
    void confirmRecusaOLoteInteiroQuandoUmaLinhaCaiNoPeriodoFechado() {
        val accountId = seedAccount("Conta");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();
        closePeriod("2025-07");

        confirmRaw(row("Uber", "25.00", "2025-07-08", cardId, categoryId),
                   row("Mercado", "80.00", "2025-08-08", cardId, categoryId))
                .statusCode(422)
                .body("code", is("CLOSED_PERIOD"));

        // Recusa antes de qualquer escrita: nem a linha de agosto entrou.
        assertEquals(0, listTransactions().size());
    }

    @Test
    void confirmImportaAsLinhasPosterioresAoFechamento() {
        val accountId = seedAccount("Conta");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();
        closePeriod("2025-07");

        confirm(row("Mercado", "80.00", "2025-08-08", cardId, categoryId)).body("created", is(1));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static String row(String description, String amount, String date, UUID cardId, UUID categoryId) {
        return installmentRow(description, amount, date, date, null, null, cardId, categoryId);
    }

    private static String installmentRow(String description, String amount, String date, String originalDate,
                                         Integer number, Integer total, UUID cardId, UUID categoryId) {
        return """
                {"description":"%s","amount":%s,"date":"%s","originalDate":"%s",
                 "installmentNumber":%s,"installmentTotal":%s,"categoryId":"%s","cardId":"%s"}
                """.formatted(description, amount, date, originalDate, number, total, categoryId, cardId);
    }

    private ValidatableResponse confirm(String... rows) {
        return confirmRaw(rows).statusCode(200);
    }

    private ValidatableResponse confirmRaw(String... rows) {
        return asTestUser()
                .body("{\"type\":\"CREDIT_CARD_INVOICE\",\"rows\":[%s]}".formatted(String.join(",", rows)))
                .when().post(path(CONFIRM))
                .then();
    }

    private List<Map<String, Object>> listTransactions() {
        return asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .extract().jsonPath().getList("$");
    }

    private static Map<String, Object> find(List<Map<String, Object>> transactions, String description) {
        return transactions.stream()
                .filter(t -> description.equals(t.get("description")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nenhuma transação \"" + description + '"'));
    }

    private void createTransaction(UUID accountId, UUID categoryId, String description, String amount, String date) {
        val body = """
                {"description":"%s","amount":%s,"date":"%s","categoryId":"%s",
                 "costCenterId":"d0000000-0000-0000-0000-000000000002",
                 "status":"confirmed","type":"expense","installments":1}
                """.formatted(description, amount, date, categoryId);
        asTestUser()
                .body(body)
                .when().post(path("/accounts/" + accountId + "/transactions"))
                .then().statusCode(201);
    }
}
