package br.cdb.feature.f006;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Importação de extrato de conta corrente pela interface Web — preview e confirm. As linhas do
 * extrato saem no JSON praticamente como o parser as produz (data, descrição, valor com sinal, tipo),
 * então este é o teste que substitui os antigos testes de unidade dos dois parsers de extrato e do
 * serviço de importação: reconhecimento do documento, sinal de débito/crédito, junção de linhas
 * quebradas, descarte de saldo/pagamento de fatura e a classificação NEW/DUPLICATE/RECONCILE.
 */
@QuarkusTest
class F006StatementImportResourceTest extends AbstractImportTest {

    private static final String BTG_2025 = "/extratos/extrato-btg-2025.txt";
    private static final String SANTANDER_202512 = "/extratos/extrato-santander-202512.txt";

    /** Extrato BTG sintético: um débito, um crédito, um saldo diário e um pagamento de fatura. */
    private static final String EXTRATO = String.join("\n",
            "Extrato de conta corrente",
            "Este é o extrato da sua conta corrente BTG Pactual",
            "Data e hora Categoria Transação Descrição Valor",
            "05/03/2025 10h42 Saúde Pagamento de boleto Odontoprev -R$ 161,43",
            "06/03/2025 02h17 Crédito e Financiamento Pix recebido Caixa Economica R$ 3.000,00",
            "07/03/2025 23h59 Saldo Diário R$ 100,00",
            "08/03/2025 11h52 Contas Pagamento de fatura do cartão Fatura do cartão BTG Pactual -R$ 232,97");

    /** Um movimento em março e outro em abril: com o fechamento em {@code 2025-03}, um de cada lado. */
    private static final String EXTRATO_DOIS_MESES = String.join("\n",
            "Extrato de conta corrente",
            "Este é o extrato da sua conta corrente BTG Pactual",
            "Data e hora Categoria Transação Descrição Valor",
            "05/03/2025 10h42 Saúde Pagamento de boleto Odontoprev -R$ 161,43",
            "06/04/2025 02h17 Crédito e Financiamento Pix recebido Caixa Economica R$ 3.000,00");

    // ── extrato BTG ────────────────────────────────────────────────────────────

    @Test
    void btgLeDocumentoComoExtratoDeContaCorrente() throws IOException {
        preview(fixturePdf(BTG_2025)).statusCode(200)
                .body("documentType", is("BANK_STATEMENT"))
                .body("issuer", is("BTG Pactual"));
    }

    @Test
    void btgDescartaSaldoPagamentoDeFaturaELinhaDeCabecalho() throws IOException {
        val rows = rows(previewFixture(BTG_2025));

        assertTrue(rows.stream().noneMatch(r -> str(r, "description").contains("Saldo Diário")));
        assertTrue(rows.stream().noneMatch(r -> str(r, "description").contains("Saldo final")));
        assertTrue(rows.stream().noneMatch(r -> str(r, "description").contains("Pagamento de fatura do cartão")));
        // A linha de cabeçalho com a data de impressão ("01/06/2026 … Lançamentos: R$ 69,64") não é movimento.
        assertTrue(rows.stream().noneMatch(r -> str(r, "date").startsWith("2026")));
        assertTrue(rows.stream().noneMatch(r -> amount(r).compareTo(new BigDecimal("69.64")) == 0));
    }

    @Test
    void btgDaSinalNegativoAosDebitosEPositivoAosCreditos() throws IOException {
        val rows = rows(previewFixture(BTG_2025));

        // Débito: Odontoprev -R$ 161,43 em 02/01/2025.
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-01-02", "Odontoprev", "-161.43")
                && "expense".equals(r.get("type"))));
        // Crédito: Pix recebido R$ 45,00 em 09/01/2025.
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-01-09", "Vanessa", "45.00")
                && "income".equals(r.get("type"))));
    }

    @Test
    void btgMantemRegistrosQuebradosEmVariasLinhas() throws IOException {
        val rows = rows(previewFixture(BTG_2025));

        // Quebra na categoria: "Impostos e / Tributos / IOF limite da conta … -R$ 2,46".
        assertTrue(rows.stream().anyMatch(r -> str(r, "description").contains("IOF limite da conta")
                && amount(r).compareTo(new BigDecimal("-2.46")) == 0));
        // Quebra na descrição: o valor fica sozinho na própria linha (R$ 2.273,90).
        assertTrue(rows.stream().anyMatch(r -> str(r, "description").contains("Secr. Da Receita Federal")
                && amount(r).compareTo(new BigDecimal("2273.90")) == 0));
        // Crédito com separador de milhar.
        assertTrue(rows.stream().anyMatch(r -> amount(r).compareTo(new BigDecimal("35700.00")) == 0));
    }

    @Test
    void btgLeTodosOsMesesDoAno() throws IOException {
        val rows = rows(previewFixture(BTG_2025));

        assertTrue(rows.size() > 100, "esperava o extrato do ano inteiro, veio " + rows.size());
        for (int month = 1; month <= 12; month++) {
            val prefix = "2025-%02d".formatted(month);
            assertTrue(rows.stream().anyMatch(r -> str(r, "date").startsWith(prefix)),
                    "nenhum movimento lido para o mês " + prefix);
        }
    }

    // ── extrato Santander ──────────────────────────────────────────────────────

    @Test
    void santanderLeDocumentoComoExtratoDeContaCorrente() throws IOException {
        preview(fixturePdf(SANTANDER_202512)).statusCode(200)
                .body("documentType", is("BANK_STATEMENT"))
                .body("issuer", is("Santander"));
    }

    @Test
    void santanderDaSinalNegativoAosDebitosEPositivoAosCreditos() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        // Crédito (sem o menos no fim): TED e Pix recebidos.
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-12-04", "TED RECEBIDA", "14098.27")));
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-12-29", "PIX RECEBIDO", "1800.00")));
        // Débito (menos no fim → negativo), com separador de milhar.
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-12-04", "PIX ENVIADO", "-250.00")));
        assertTrue(rows.stream().anyMatch(r -> movement(r, "2025-12-04", "BANCO EXEMPLO BRASIL", "-1242.31")));
    }

    @Test
    void santanderJuntaDescricaoQuebradaEIgnoraOSaldoCorrente() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        // Tarifa: linha do tipo + continuação PERIODO + linha de valor com movimento e saldo.
        assertTrue(rows.stream().anyMatch(r ->
                "2025-12-01".equals(r.get("date"))
                        && str(r, "description").contains("JUROS SALDO UTILIZ ATE LIMITE")
                        && str(r, "description").contains("PERIODO: 01/11 A 30/11/25")
                        && amount(r).compareTo(new BigDecimal("-23.93")) == 0));
        // O saldo corrente (552,80) da mesma linha nunca pode virar valor de movimento.
        assertTrue(rows.stream().noneMatch(r -> amount(r).abs().compareTo(new BigDecimal("552.80")) == 0));
    }

    @Test
    void santanderTrataDataDeOrigemDoLojistaComoContinuacao() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        // "02/12 COMPRA CARTAO DEB MC" / "28/11 PADARIA EXEMPLO LTDA" / "330759 23,00-": a linha do
        // lojista começa com data de origem mas é continuação, então o registro fica em 02/12.
        assertTrue(rows.stream().anyMatch(r ->
                "2025-12-02".equals(r.get("date"))
                        && str(r, "description").contains("COMPRA CARTAO DEB MC")
                        && str(r, "description").contains("PADARIA EXEMPLO")
                        && amount(r).compareTo(new BigDecimal("-23.00")) == 0));
        assertTrue(rows.stream().noneMatch(r -> str(r, "date").startsWith("2025-11")),
                "a data de origem não pode gerar um registro de novembro");
    }

    @Test
    void santanderCarregaADataAtravesDaQuebraDePagina() throws IOException {
        // Movimento impresso depois de um bloco de cabeçalho de página: herda a última data vista.
        assertTrue(rows(previewFixture(SANTANDER_202512)).stream()
                .anyMatch(r -> movement(r, "2025-12-04", "CONTA DE AGUA E ESGOTO", "-165.64")));
    }

    @Test
    void santanderLeValorNaMesmaLinhaDaDescricao() throws IOException {
        // "REMUNERACAO APLICACAO AUTOMATICA - 0,01 1.834,81-": descrição e valor na mesma linha.
        assertTrue(rows(previewFixture(SANTANDER_202512)).stream()
                .anyMatch(r -> movement(r, "2025-12-11", "REMUNERACAO APLICACAO AUTOMATICA", "0.01")));
    }

    @Test
    void santanderDescartaOsPagamentosAgregadosDeFaturaDeCartao() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        // Débito automático do próprio cartão e a fatura paga por boleto.
        assertTrue(rows.stream().noneMatch(r -> str(r, "description").toUpperCase().contains("PAGAMENTO CARTAO")));
        assertTrue(rows.stream().noneMatch(r -> amount(r).abs().compareTo(new BigDecimal("13366.18")) == 0));
        assertTrue(rows.stream().noneMatch(r -> str(r, "description").toUpperCase().contains("FATURA CARTAO")));
        assertTrue(rows.stream().noneMatch(r -> amount(r).abs().compareTo(new BigDecimal("3076.08")) == 0));
    }

    @Test
    void santanderIgnoraTudoForaDaTabelaDeMovimentacao() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        // As linhas de "SALDO EM" (abertura/fechamento) não são movimentos.
        assertTrue(rows.stream().noneMatch(r -> str(r, "description").contains("SALDO EM")));
        // As tabelas seguintes ("Saldos por Período", "Débito Automático", "Transferências") têm data
        // e valor próprios e não podem vazar: nem as colunas zeradas, nem o valor só de Provisão, nem
        // os seus cabeçalhos como descrição.
        assertTrue(rows.stream().noneMatch(r -> amount(r).signum() == 0));
        assertTrue(rows.stream().noneMatch(r -> amount(r).abs().compareTo(new BigDecimal("134.04")) == 0));
        assertTrue(rows.stream().noneMatch(r -> {
            val description = str(r, "description");
            return description.contains("Disponível") || description.contains("Favorecido")
                    || description.contains("Realizado") || description.contains("Saldos por");
        }));
    }

    @Test
    void santanderLeCadaMovimentoDoMesUmaUnicaVez() throws IOException {
        val rows = rows(previewFixture(SANTANDER_202512));

        assertEquals(23, rows.size());
        assertTrue(rows.stream().allMatch(r -> str(r, "date").startsWith("2025-12")));
    }

    // ── seleção de conta e classificação ───────────────────────────────────────

    @Test
    void previewEscolheAContaUnicaEClassificaTodaLinhaComoNova() throws IOException {
        val accountId = seedAccount("Conta BTG");

        val json = previewOf(EXTRATO);

        assertEquals(accountId.toString(), json.getString("selectedAccountId"));
        assertEquals(List.of(accountId.toString()), json.getList("candidateAccounts.id"));
        val rows = rows(json);
        assertEquals(2, rows.size(), "saldo diário e pagamento de fatura são descartados");
        assertTrue(rows.stream().allMatch(r -> "NEW".equals(r.get("state"))));
        assertEquals(0, amount(byDescription(rows, "Odontoprev")).compareTo(new BigDecimal("-161.43")));
        assertEquals("expense", byDescription(rows, "Odontoprev").get("type"));
        assertEquals(0, amount(byDescription(rows, "Caixa Economica")).compareTo(new BigDecimal("3000.00")));
        assertEquals("income", byDescription(rows, "Caixa Economica").get("type"));
    }

    @Test
    void previewMarcaComoDuplicataOMovimentoJaLancado() throws IOException {
        val accountId = seedAccount("Conta BTG");
        // Mesma conta, data, valor e descrição do movimento — o extrato descreve o lançamento com a
        // categoria impressa junto, então a descrição vem da própria leitura.
        val descricao = str(byDescription(rows(previewOf(EXTRATO)), "Odontoprev"), "description");
        createTransaction(accountId, descricao, "-161.43", "2025-03-05", "confirmed");

        val rows = rows(previewOf(EXTRATO));

        assertEquals("DUPLICATE", byDescription(rows, "Odontoprev").get("state"));
        assertEquals("NEW", byDescription(rows, "Caixa Economica").get("state"));
    }

    @Test
    void previewMarcaParaConciliarOLancamentoManualPendenteDentroDaJanela() throws IOException {
        val accountId = seedAccount("Conta BTG");
        // Mesmo valor, um dia antes: dentro da janela de conciliação.
        createTransaction(accountId, "Dentista", "-161.43", "2025-03-04", "pending");

        val odontoprev = byDescription(rows(previewOf(EXTRATO)), "Odontoprev");

        assertEquals("RECONCILE", odontoprev.get("state"));
        assertEquals("Dentista", odontoprev.get("reconcileDescription"));
    }

    // ── fechamento contábil ────────────────────────────────────────────────────

    @Test
    void previewMarcaAsLinhasDentroDoPeriodoFechado() throws IOException {
        seedAccount("Conta BTG");
        closePeriod("2025-03");

        val json = previewOf(EXTRATO_DOIS_MESES);

        assertEquals("2025-03", json.getString("closingPeriod"));
        assertEquals(Boolean.TRUE, byDescription(rows(json), "Odontoprev").get("closed"));
        assertEquals(Boolean.FALSE, byDescription(rows(json), "Caixa Economica").get("closed"));
    }

    @Test
    void previewNaoMarcaLinhaAlgumaSemFechamento() throws IOException {
        seedAccount("Conta BTG");

        val json = previewOf(EXTRATO_DOIS_MESES);

        assertNull(json.getString("closingPeriod"));
        assertTrue(rows(json).stream().noneMatch(r -> Boolean.TRUE.equals(r.get("closed"))));
    }

    @Test
    void confirmRecusaOLoteInteiroQuandoUmaLinhaCaiNoPeriodoFechado() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        val rows = rows(previewOf(EXTRATO_DOIS_MESES));
        closePeriod("2025-03");

        confirmStatementRaw(accountId, categoryId, rows)
                .statusCode(422)
                .body("code", is("CLOSED_PERIOD"));

        // Recusa antes de qualquer escrita: nem a linha de abril entrou.
        asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void confirmImportaAsLinhasPosterioresAoFechamento() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        val abertas = rows(previewOf(EXTRATO_DOIS_MESES)).stream()
                .filter(r -> str(r, "description").contains("Caixa Economica"))
                .toList();
        closePeriod("2025-03");

        confirmStatement(accountId, categoryId, abertas).body("created", is(1));
    }

    // ── confirm ────────────────────────────────────────────────────────────────

    @Test
    void confirmInsereAsLinhasNovasComSinalTipoEStatus() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();

        confirmStatement(accountId, categoryId, rows(previewOf(EXTRATO)))
                .body("created", is(2))
                .body("reconciled", is(0))
                .body("skipped", is(0));

        asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .body("size()", is(2))
                .body("find { it.description.contains('Odontoprev') }.amount", is(-161.43f))
                .body("find { it.description.contains('Odontoprev') }.type", is("expense"))
                .body("find { it.description.contains('Odontoprev') }.status", is("confirmed"))
                .body("find { it.description.contains('Caixa Economica') }.amount", is(3000.00f))
                .body("find { it.description.contains('Caixa Economica') }.type", is("income"));
    }

    @Test
    void confirmPromoveOManualPendenteSemInserirOutraLinha() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        // Categoria própria pro manual pendente, distinta da escolhida na linha do extrato — a
        // conciliação deve substituir pela categoria escolhida, não preservar a antiga.
        val manualId = createTransaction(accountId, "Dentista", "-161.43", "2025-03-04", "pending");

        confirmStatement(accountId, categoryId, rows(previewOf(EXTRATO)))
                .body("created", is(1))
                .body("reconciled", is(1))
                .body("skipped", is(0));

        // A conciliação não insere uma segunda linha: promove a que já existia, e aplica a
        // categoria escolhida na linha do extrato à transação existente.
        asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .body("size()", is(2))
                .body("find { it.id == '%s' }.status".formatted(manualId), is("confirmed"))
                .body("find { it.id == '%s' }.categoryId".formatted(manualId), is(categoryId.toString()));
    }

    @Test
    void confirmNaoConciliaForaDaJanelaDeDatas() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        // Quatro dias antes: fora da janela de três dias.
        val manualId = createTransaction(accountId, "Dentista", "-161.43", "2025-03-01", "pending");

        confirmStatement(accountId, categoryId, rows(previewOf(EXTRATO)))
                .body("created", is(2))
                .body("reconciled", is(0));

        asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .body("size()", is(3))
                .body("find { it.id == '%s' }.status".formatted(manualId), is("pending"));
    }

    @Test
    void confirmPulaOMovimentoJaImportado() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        val rows = rows(previewOf(EXTRATO));

        confirmStatement(accountId, categoryId, rows).body("created", is(2));
        // Reimportar o mesmo extrato não cria nada.
        confirmStatement(accountId, categoryId, rows)
                .body("created", is(0))
                .body("reconciled", is(0))
                .body("skipped", is(2));

        asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .body("size()", is(2));
    }

    @Test
    void confirmaLinhaComTagsGravaOVinculoEmF004TransactionTag() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val categoryId = seedLeafCategory();
        val tagId = seedTag("Viagem");
        val odontoprev = byDescription(rows(previewOf(EXTRATO)), "Odontoprev");

        val body = """
                {"type":"BANK_STATEMENT","accountId":"%s","rows":[
                  {"description":"%s","amount":%s,"date":"%s","transactionType":"%s",
                   "categoryId":"%s","tagIds":["%s"]}]}
                """.formatted(accountId, odontoprev.get("description"), amount(odontoprev), odontoprev.get("date"),
                odontoprev.get("type"), categoryId, tagId);

        asTestUser().body(body).when().post(path(CONFIRM)).then().statusCode(200).body("created", is(1));

        val persisted = asTestUser().when().get(path("/accounts/transactions")).then().statusCode(200)
                .extract().jsonPath().getList("$", Map.class).stream()
                .filter(t -> String.valueOf(t.get("description")).contains("Odontoprev"))
                .findFirst().orElseThrow(() -> new AssertionError("transação não encontrada"));

        assertEquals(List.of(tagId.toString()), persisted.get("tagIds"));
    }

    @Test
    void confirmExigeAConta() throws IOException {
        val categoryId = seedLeafCategory();
        val body = """
                {"type":"BANK_STATEMENT","rows":[
                  {"description":"Odontoprev","amount":-161.43,"date":"2025-03-05",
                   "transactionType":"expense","categoryId":"%s"}]}
                """.formatted(categoryId);

        asTestUser()
                .body(body)
                .when().post(path(CONFIRM))
                .then().statusCode(422)
                .body("code", is("ACCOUNT_REQUIRED"));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static boolean movement(Map<String, Object> row, String date, String description, String amount) {
        return date.equals(row.get("date"))
                && str(row, "description").contains(description)
                && amount(row).compareTo(new BigDecimal(amount)) == 0;
    }

    private static Map<String, Object> byDescription(List<Map<String, Object>> rows, String description) {
        return rows.stream()
                .filter(r -> str(r, "description").contains(description))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nenhuma linha com descrição \"" + description + '"'));
    }

    private ValidatableResponse confirmStatement(
            UUID accountId, UUID categoryId, List<Map<String, Object>> previewRows) {
        return confirmStatementRaw(accountId, categoryId, previewRows).statusCode(200);
    }

    private ValidatableResponse confirmStatementRaw(
            UUID accountId, UUID categoryId, List<Map<String, Object>> previewRows) {
        val rows = previewRows.stream().map(r -> """
                {"description":"%s","amount":%s,"date":"%s","transactionType":"%s","categoryId":"%s"}
                """.formatted(r.get("description"), amount(r), r.get("date"), r.get("type"), categoryId)).toList();
        val body = """
                {"type":"BANK_STATEMENT","accountId":"%s","rows":[%s]}
                """.formatted(accountId, String.join(",", rows));

        return asTestUser()
                .body(body)
                .when().post(path(CONFIRM))
                .then();
    }

    private String createTransaction(UUID accountId, String description, String amount, String date, String status) {
        val body = """
                {"description":"%s","amount":%s,"date":"%s","categoryId":"%s",
                 "costCenterId":"d0000000-0000-0000-0000-000000000002",
                 "status":"%s","type":"expense","installments":1}
                """.formatted(description, amount, date, seedLeafCategory(), status);
        return asTestUser()
                .body(body)
                .when().post(path("/accounts/" + accountId + "/transactions"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
    }
}
