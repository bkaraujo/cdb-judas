package br.cdb.feature.f006;

import io.quarkus.test.junit.QuarkusTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Preview de fatura de cartão pela interface Web: {@code POST /accounts/transactions/import/preview}
 * com o PDF entrando e o JSON de resposta como única superfície observada. Cobre, pelo que o endpoint
 * expõe, o que os antigos testes de unidade de parser/expansor/casador/assinatura verificavam por
 * dentro — leitura das faturas BTG e Santander anonimizadas, expansão do carnê de parcelas, sugestão
 * de cartão por {@code last4} e marcação de duplicata.
 */
@QuarkusTest
class F006InvoiceImportResourceTest extends AbstractImportTest {

    private static final String BTG_ABRIL = "/faturas/fatura-btg-abril.txt";
    private static final String BTG_MAIO = "/faturas/fatura-btg-maio.txt";
    private static final String BTG_FEV25 = "/faturas/fatura-btg-fev25.txt";
    private static final String SANTANDER_ABRIL = "/faturas/fatura-santander-abril.txt";
    private static final String SANTANDER_MAIO = "/faturas/fatura-santander-maio.txt";

    // ── BTG ────────────────────────────────────────────────────────────────────

    @Test
    void btgLeDocumentoComoFaturaESoOsCartoesComCompras() throws IOException {
        // As compras ficam sob os sub-cartões (físico/virtual), não sob o "final 5115" da conta.
        preview(fixturePdf(BTG_ABRIL)).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE"))
                .body("issuer", is("BTG Pactual"))
                .body("last4s", contains("0020", "9822"));
    }

    @Test
    void btgMantemSoAsComprasDeCadaCartao() throws IOException {
        val json = previewFixture(BTG_ABRIL);

        assertEquals(List.of("0020", "9822"), json.getList("rows.last4").stream().distinct().toList());
        assertTrue(json.getList("rows.amount", Float.class).stream().allMatch(a -> a > 0),
                "estorno/crédito não vira linha de compra");
    }

    @Test
    void btgExpandeALinhaParceladaNoCarneInteiro() throws IOException {
        val parcelas = rowsWithDescription(previewFixture(BTG_ABRIL), "Amazonmktplc Megabytem");

        // Impressa uma única vez como "(9/10)15 Jul"; o carnê completo é reconstruído.
        assertEquals(10, parcelas.size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ints(parcelas, "installmentNumber"));
        assertTrue(parcelas.stream().allMatch(r -> Integer.valueOf(10).equals(r.get("installmentTotal"))));
        assertTrue(parcelas.stream().allMatch(r -> "0020".equals(r.get("last4"))));
        assertTrue(parcelas.stream().allMatch(r -> amount(r).compareTo(new BigDecimal("72.99")) == 0));
        // Período impresso na fatura (abril/2026) + deslocamento da parcela ancoram o ano da compra.
        assertTrue(parcelas.stream().allMatch(r -> "2025-07-15".equals(r.get("originalDate"))));
        assertEquals(List.of("2025-07-15", "2026-04-15"),
                List.of(parcelas.getFirst().get("date"), parcelas.getLast().get("date")));
        assertEquals(1L, parcelas.stream().map(r -> r.get("groupId")).distinct().count(),
                "todas as parcelas do mesmo carnê compartilham um groupId");
    }

    @Test
    void btgLinhaAVistaNaoTemParcela() throws IOException {
        val microsoft = onlyRowWithDescription(previewFixture(BTG_ABRIL), "Microsoft");

        assertNull(microsoft.get("installmentNumber"));
        assertNull(microsoft.get("installmentTotal"));
        assertNull(microsoft.get("groupId"));
        assertEquals("2026-03-09", microsoft.get("date"));
        assertEquals(microsoft.get("date"), microsoft.get("originalDate"), "à-vista: data == data original");
        assertEquals(0, amount(microsoft).compareTo(new BigDecimal("60.00")));
        assertEquals("9822", microsoft.get("last4"));
    }

    @Test
    void btgDescartaPagamentosParcelamentoDeFaturaCreditosEResumo() throws IOException {
        val descriptions = previewFixture(BTG_ABRIL).getList("rows.description", String.class);

        assertTrue(descriptions.stream().noneMatch(d -> d.contains("Pagamento de fatura")));
        assertTrue(descriptions.stream().noneMatch(d -> d.startsWith("Parcelamento da Fatura")));
        assertTrue(descriptions.stream().noneMatch(d -> d.contains("Desconto")));
        assertTrue(descriptions.stream().noneMatch(d -> d.contains("Cancelamento")));
    }

    @Test
    void btgParcelaAvancaNoMesSeguinteSemMudarACompraOriginal() throws IOException {
        val json = previewFixture(BTG_MAIO);
        val parcelas = rowsWithDescription(json, "Amazonmktplc Megabytem");

        // "5115" (só crédito) aparece antes de "0020"/"9822" na fatura, então entra primeiro na lista.
        assertEquals(List.of("5115", "0020", "9822"), json.getList("rows.last4").stream().distinct().toList());
        // Mesma compra de abril, agora cobrada como 10/10: data original e valor intactos.
        assertEquals(10, parcelas.size());
        assertTrue(parcelas.stream().allMatch(r -> "2025-07-15".equals(r.get("originalDate"))));
        assertTrue(parcelas.stream().allMatch(r -> amount(r).compareTo(new BigDecimal("72.99")) == 0));
    }

    @Test
    void btgDescartaLinhaInternacionalEmDolar() throws IOException {
        val json = previewFixture(BTG_MAIO);

        // Linha em US$: o valor em BRL não está na linha, então não é emitida.
        assertTrue(json.getList("rows.description", String.class).stream()
                .noneMatch(d -> d.contains("Amazon Web Services")));
    }

    @Test
    void btgLeCreditoDoBlocoComoLinhaDeReceita() throws IOException {
        // O cartão final 5115 só tem crédito (nenhuma compra): antes do fix o bloco inteiro caía;
        // agora cada linha vira uma entrada type=income, sem expandir em carnê.
        val json = previewFixture(BTG_MAIO);

        assertTrue(json.getList("last4s", String.class).contains("5115"));
        val rows = rowsWithLast4(json, "5115");
        assertEquals(8, rows.size());
        assertTrue(rows.stream().allMatch(r -> "income".equals(str(r, "type"))));
        assertTrue(rows.stream().allMatch(r -> r.get("installmentNumber") == null),
                "crédito não expande em carnê, mesmo com \"De 12\" no texto da descrição");
        val total = rows.stream().map(r -> amount(r)).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(new BigDecimal("891.54")));
    }

    @Test
    void btgRecuperaComprasDoCartaoQueAtravessamMobiliaDePaginaNoMaio() throws IOException {
        // Regressão: "R$ 1.465,58" (subtotal solto após as 10 primeiras linhas) fechava o bloco do
        // 9822 antes do fix, e o rodapé/cabeçalho repetido entre páginas nunca o reabria — as linhas
        // seguintes (à vista + o estorno) se perdiam em silêncio. Todas aqui são à vista (1/1), então
        // cada uma vira exatamente uma linha no preview — sem multiplicador de parcela para confundir.
        val json = previewFixture(BTG_MAIO);

        val ceopag = onlyRowWithDescription(json, "Ceopag");
        assertEquals(0, amount(ceopag).compareTo(new BigDecimal("115.00")));
        assertEquals("9822", ceopag.get("last4"));

        val mercadoLivre = onlyRowWithDescription(json, "Mercado Livre");
        assertEquals(0, amount(mercadoLivre).compareTo(new BigDecimal("389.90")));
        assertEquals("expense", str(mercadoLivre, "type"));

        val estorno = onlyRowWithDescription(json, "Cancelamento - Mercado Livre");
        assertEquals(0, amount(estorno).compareTo(new BigDecimal("389.90")));
        assertEquals("income", str(estorno, "type"));

        // 3 Uber já existiam antes do fix (25/04, 30/04, 01/05); as outras 4 (10/04 ×2, 02/05, 04/05)
        // ficavam do outro lado da quebra de página.
        assertEquals(7, rowsWithDescription(json, "Uber").size());
    }

    @Test
    void btgMantemBlocoDoCartaoAtravesDeQuebraDePaginaNoPdfReal() throws IOException {
        // Fatura real de 5 páginas: as 86 linhas do cartão final 0020 atravessam uma quebra de
        // página no meio (rodapé + cabeçalho repetido + um "R$ 6.853,81" solto que, coincidência,
        // já bate com o total do cartão sem ter visto todas as linhas — não pode ser sentinela).
        val rows = rowsWithLast4(previewFixture(BTG_FEV25), "0020");
        assertEquals(86, rows.size());
        val total = rows.stream().map(r -> amount(r)).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(new BigDecimal("6853.81")));
    }

    @Test
    void btgLeFaturaRealComCreditoLiquidandoNoTotalDoCartao() throws IOException {
        // Cartão final 5115 na fatura real: a fatura cobrou 1 parcela de "Caedu (2/2)" (199,95) e
        // 1 crédito (13,08) — a parcela do período menos o crédito fecha em 186,87, exatamente o
        // "Total do cartão" impresso (decisão 1: reconciliar pelo que a fatura cobrou, não pela soma
        // do carnê expandido — o preview reconstrói o carnê inteiro, então "Caedu" vira 2 linhas).
        val json = previewFixture(BTG_FEV25);
        val rows = rowsWithLast4(json, "5115");
        assertEquals(3, rows.size(), "2 parcelas do carnê reconstruído (Caedu 1/2, 2/2) + 1 crédito");

        val caedu = rowsWithDescription(json, "Caedu");
        assertEquals(2, caedu.size());
        assertTrue(caedu.stream().allMatch(r -> "expense".equals(str(r, "type"))));
        assertTrue(caedu.stream().allMatch(r -> amount(r).compareTo(new BigDecimal("199.95")) == 0));
        assertEquals(List.of(1, 2), ints(caedu, "installmentNumber"));
        assertEquals(1L, caedu.stream().map(r -> r.get("groupId")).distinct().count());

        val credit = onlyRowWithDescription(json, "Benefício do cartão BTG Pactual");
        assertEquals(0, amount(credit).compareTo(new BigDecimal("13.08")));
        assertEquals("income", str(credit, "type"));
        assertNull(credit.get("installmentNumber"), "crédito não expande em carnê");

        // A parcela do período (199,95, não a soma do carnê) menos o crédito bate com o total impresso.
        val net = amount(caedu.getFirst()).subtract(amount(credit));
        assertEquals(0, net.compareTo(new BigDecimal("186.87")));
    }

    @Test
    void confirmaLinhaDeCreditoEPersisteComoReceita() throws IOException {
        // Round-trip completo: hoje só o JSON do preview é testado nesse caminho — o bug relatado
        // (crédito voltando como despesa) escapava porque nenhum teste lia a transação persistida.
        val accountId = seedAccount("Conta BTG");
        val cardId = seedCard(accountId, "5115");
        val categoryId = seedLeafCategory();

        val credit = onlyRowWithDescription(previewFixture(BTG_FEV25), "Benefício do cartão BTG Pactual");
        confirmInvoice(List.of(credit), cardId, categoryId);

        val persisted = asTestUser()
                .when().get(path("/accounts/transactions"))
                .then().statusCode(200)
                .extract().jsonPath().getList("$", Map.class).stream()
                .filter(t -> "Benefício do cartão BTG Pactual".equals(t.get("description")))
                .findFirst().orElseThrow(() -> new AssertionError("transação de crédito não encontrada"));

        assertEquals("income", persisted.get("type"));
    }

    @Test
    void confirmaLinhaComTagsGravaOVinculoEmF004TransactionTag() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val cardId = seedCard(accountId, "9822");
        val categoryId = seedLeafCategory();
        val tagId = seedTag("Viagem");

        val microsoft = onlyRowWithDescription(previewFixture(BTG_ABRIL), "Microsoft");
        val body = """
                {"type":"CREDIT_CARD_INVOICE","rows":[
                  {"description":"%s","amount":%s,"date":"%s","originalDate":"%s",
                   "installmentNumber":null,"installmentTotal":null,"transactionType":"expense",
                   "categoryId":"%s","cardId":"%s","tagIds":["%s"]}]}
                """.formatted(microsoft.get("description"), amount(microsoft), microsoft.get("date"), microsoft.get("originalDate"),
                categoryId, cardId, tagId);

        asTestUser().body(body).when().post(path(CONFIRM)).then().statusCode(200).body("created", is(1));

        val persisted = asTestUser().when().get(path("/accounts/transactions")).then().statusCode(200)
                .extract().jsonPath().getList("$", Map.class).stream()
                .filter(t -> "Microsoft".equals(t.get("description")))
                .findFirst().orElseThrow(() -> new AssertionError("transação não encontrada"));

        assertEquals(List.of(tagId.toString()), persisted.get("tagIds"));
    }

    // ── Santander ──────────────────────────────────────────────────────────────

    @Test
    void santanderLeOLast4DeCadaCartaoNoPanMascarado() throws IOException {
        preview(fixturePdf(SANTANDER_ABRIL)).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE"))
                .body("issuer", is("Santander"))
                .body("last4s", contains("1258", "2884"));
    }

    @Test
    void santanderExpandeALinhaDeParcelamento() throws IOException {
        // A fatura tem duas compras DECATHLON (8/10 e 2/2): esta é a de dez parcelas.
        val parcelas = rowsWithDescription(previewFixture(SANTANDER_ABRIL), "DECATHLON").stream()
                .filter(r -> Integer.valueOf(10).equals(r.get("installmentTotal")))
                .toList();

        assertEquals(10, parcelas.size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ints(parcelas, "installmentNumber"));
        assertTrue(parcelas.stream().allMatch(r -> "2025-08-03".equals(r.get("originalDate"))));
        assertTrue(parcelas.stream().allMatch(r -> amount(r).compareTo(new BigDecimal("111.98")) == 0));
        assertTrue(parcelas.stream().allMatch(r -> "1258".equals(r.get("last4"))));
    }

    @Test
    void santanderDespesaAVistaNaoTemParcela() throws IOException {
        val netflix = onlyRowWithDescription(previewFixture(SANTANDER_ABRIL), "NETFLIX COM");

        assertNull(netflix.get("installmentNumber"));
        assertEquals("2026-03-06", netflix.get("date"));
        assertEquals(0, amount(netflix).compareTo(new BigDecimal("72.80")));
        assertEquals("1258", netflix.get("last4"));
    }

    @Test
    void santanderMantemAnuidadeEIofExternoAncoradoNaCompraQueOGerou() throws IOException {
        val rows = rows(previewFixture(SANTANDER_ABRIL));

        // ANUIDADE DIFERENCIADA (R$ 0,00) permanece como lançamento.
        assertTrue(rows.stream().anyMatch(r ->
                str(r, "description").startsWith("ANUIDADE")
                        && amount(r).compareTo(new BigDecimal("0.00")) == 0));
        // O IOF externo não tem data própria: herda a da compra em moeda estrangeira (VIKI COM, 20/03).
        assertTrue(rows.stream().anyMatch(r ->
                str(r, "description").contains("IOF DESPESA")
                        && amount(r).compareTo(new BigDecimal("1.41")) == 0
                        && "2026-03-20".equals(r.get("date"))));
    }

    @Test
    void santanderDescartaPagamentosCreditosEstornosEResumo() throws IOException {
        val json = previewFixture(SANTANDER_ABRIL);
        val descriptions = json.getList("rows.description", String.class);

        assertTrue(descriptions.stream().noneMatch(d -> d.contains("PAGAMENTO DE FATURA")));
        assertTrue(descriptions.stream().noneMatch(d -> d.contains("Saldo")));
        assertTrue(json.getList("rows.amount", Float.class).stream().allMatch(a -> a >= 0));
    }

    @Test
    void santanderLeTodosOsCartoesDosDoisPortadores() throws IOException {
        preview(fixturePdf(SANTANDER_MAIO)).statusCode(200)
                .body("last4s", contains("4628", "1258", "2884", "1922", "8376"));
    }

    @Test
    void santanderParcelaAvancaNoMesSeguinteSemMudarACompraOriginal() throws IOException {
        val json = previewFixture(SANTANDER_MAIO);

        // DECATHLON agora é 09/10; a data da compra continua sendo 03/08, como em abril.
        assertTrue(rowsWithDescription(json, "DECATHLON").stream()
                .allMatch(r -> "2025-08-03".equals(r.get("originalDate"))));
        // Santander varia os centavos por parcela: 037 - DF TAGUATINGA CT cobra 231,79 na 3/3.
        assertTrue(rowsWithDescription(json, "037 - DF TAGUATINGA CT").stream()
                .allMatch(r -> amount(r).compareTo(new BigDecimal("231.79")) == 0));
    }

    // ── casamento de cartão ────────────────────────────────────────────────────

    @Test
    void sugereOCartaoQueCasaComOLast4DaLinha() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val visa = seedCard(accountId, "0020");
        seedCard(accountId, "9999");

        val json = previewFixture(BTG_ABRIL);

        // Só o cartão presente na fatura é oferecido; o 9999 não aparece.
        assertEquals(List.of(visa.toString()), json.getList("candidateCards.id"));
        assertTrue(rowsWithLast4(json, "0020").stream()
                .allMatch(r -> visa.toString().equals(r.get("suggestedCardId"))));
        assertTrue(rowsWithLast4(json, "9822").stream().allMatch(r -> r.get("suggestedCardId") == null),
                "cartão não cadastrado não gera sugestão");
    }

    @Test
    void naoSugereCartaoQuandoNenhumEstaCadastrado() throws IOException {
        val json = previewFixture(BTG_ABRIL);

        assertTrue(json.getList("candidateCards").isEmpty());
        assertTrue(rows(json).stream().allMatch(r -> r.get("suggestedCardId") == null));
    }

    @Test
    void naoSugereCartaoQuandoDoisCartoesCompartilhamOLast4() throws IOException {
        // Mesmo last4 em duas contas: candidato ambíguo, nenhuma sugestão automática.
        val titular = seedCard(seedAccount("Conta A"), "0020");
        val adicional = seedCard(seedAccount("Conta B"), "0020");

        val json = previewFixture(BTG_ABRIL);

        assertEquals(List.of(titular.toString(), adicional.toString()),
                json.getList("candidateCards.id"), "os dois continuam ofertados para escolha manual");
        assertTrue(rows(json).stream().allMatch(r -> r.get("suggestedCardId") == null));
    }

    // ── ancoragem de ano e status ──────────────────────────────────────────────

    @Test
    void periodoDaFaturaAncoraOAnoMesmoImportandoMuitoDepois() throws IOException {
        // Regressão: importar em 2026 uma fatura de 2020 carimbava as transações com 2026.
        val row = onlyRowWithDescription(
                previewOf(btgInvoice(YearMonth.of(2020, 2), "R$ 199,90Amazon07 Fev")), "Amazon");

        assertEquals("2020-02-07", row.get("date"));
        assertEquals("2020-02-07", row.get("originalDate"));
    }

    @Test
    void statusSeparaAParcelaDoMesCorrenteDasFuturas() throws IOException {
        // Período = mês corrente: a 1ª parcela cai neste mês (confirmada), as três seguintes no futuro.
        val period = YearMonth.now();
        val json = previewOf(btgInvoice(period, "R$ 100,00Curso (1/4)" + dayAndMonth(period.atDay(15))));

        val parcelas = rowsWithDescription(json, "Curso");
        assertEquals(4, parcelas.size());
        assertEquals(List.of("confirmed", "scheduled", "scheduled", "scheduled"),
                parcelas.stream().map(r -> str(r, "status")).toList());
    }

    // ── duplicatas ─────────────────────────────────────────────────────────────

    @Test
    void marcaTodasAsParcelasDoCarneJaImportadoComoDuplicadas() throws IOException {
        val accountId = seedAccount("Conta BTG");
        val cardId = seedCard(accountId, "0020");
        val categoryId = seedLeafCategory();

        // 1ª importação: confirma o carnê inteiro que o preview sugeriu.
        confirmInvoice(rowsWithDescription(previewFixture(BTG_ABRIL), "Amazonmktplc Megabytem"), cardId, categoryId);

        // 2ª leitura do mesmo PDF: o groupId já existe, então todas as parcelas vêm marcadas.
        val second = rowsWithDescription(previewFixture(BTG_ABRIL), "Amazonmktplc Megabytem");
        assertTrue(second.stream().allMatch(r -> Boolean.TRUE.equals(r.get("duplicate"))));
    }

    @Test
    void marcaAVistaDuplicadaIgnorandoCaixaEEspacos() throws IOException {
        val accountId = seedAccount("Conta BTG");
        seedCard(accountId, "9822");
        // Mesma conta, data e valor; a descrição só difere em caixa e espaços.
        createTransaction(accountId, seedLeafCategory(), "  microsoft  ", "-60.00", "2026-03-09");

        val microsoft = onlyRowWithDescription(previewFixture(BTG_ABRIL), "Microsoft");
        assertEquals(Boolean.TRUE, microsoft.get("duplicate"));
    }

    @Test
    void naoMarcaDuplicataQuandoNenhumCartaoFoiCasado() throws IOException {
        val accountId = seedAccount("Conta BTG");
        createTransaction(accountId, seedLeafCategory(), "Microsoft", "-60.00", "2026-03-09");

        // Sem cartão cadastrado não há conta de destino: a comparação nem chega a acontecer.
        val microsoft = onlyRowWithDescription(previewFixture(BTG_ABRIL), "Microsoft");
        assertEquals(Boolean.FALSE, microsoft.get("duplicate"));
    }

    // ── documento sintético ────────────────────────────────────────────────────

    private static final String[] MONTHS = {
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private static final String[] MONTH_ABBR = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};

    /** Fatura BTG mínima com período impresso — o cabeçalho que ancora o ano das compras. */
    private static String btgInvoice(YearMonth period, String... chargeLines) {
        val header = String.join("\n",
                "BTG Pactual S.A",
                "CNPJ 30.306.294/0001-45",
                "Fatura de %s de %d".formatted(MONTHS[period.getMonthValue() - 1], period.getYear()),
                "Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 0,00",
                "Total de compras e despesas");
        return header + "\n" + String.join("\n", chargeLines) + "\nR$ 0,00";
    }

    /** O {@code "15 Ago"} que o BTG imprime no fim da linha de lançamento. */
    private static String dayAndMonth(LocalDate date) {
        return "%02d %s".formatted(date.getDayOfMonth(), MONTH_ABBR[date.getMonthValue() - 1]);
    }

    // ── confirmação/seed via HTTP ──────────────────────────────────────────────

    private void confirmInvoice(List<Map<String, Object>> previewRows, UUID cardId, UUID categoryId) {
        val rows = previewRows.stream().map(r -> """
                {"description":"%s","amount":%s,"date":"%s","originalDate":"%s",
                 "installmentNumber":%s,"installmentTotal":%s,"transactionType":"%s","categoryId":"%s","cardId":"%s"}
                """.formatted(r.get("description"), amount(r), r.get("date"), r.get("originalDate"),
                r.get("installmentNumber"), r.get("installmentTotal"), r.get("type"), categoryId, cardId)).toList();

        asTestUser()
                .body("{\"type\":\"CREDIT_CARD_INVOICE\",\"rows\":[%s]}".formatted(String.join(",", rows)))
                .when().post(path(CONFIRM))
                .then().statusCode(200)
                .body("created", is(previewRows.size()));
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
