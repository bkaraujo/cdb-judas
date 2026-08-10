package br.cdb.feature.f007;

import br.cdb.BaseHttpTest;
import br.cdb.PdfFixtures;
import br.cdb.core.web.HTTPRequest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.val;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fixture comum dos testes de importação de {@code f007}, todos escritos contra a interface Web: a
 * única forma de exercitar parser, expansão de parcelas, casamento de cartão e dedup é enviar um PDF
 * ao endpoint de preview e ler o JSON de volta — {@link PdfFixtures} rerenderiza em PDF os fixtures
 * de texto anonimizados ({@code /faturas}, {@code /extratos}), com round-trip fiel linha a linha.
 */
abstract class AbstractImportTest extends BaseHttpTest {

    protected static final String PREVIEW = "/accounts/transactions/import/preview";
    protected static final String CONFIRM = "/accounts/transactions/import/confirm";

    /** Sem o {@code contentType(JSON)} padrão de {@link #asTestUser()}: o multipart define o seu próprio. */
    protected RequestSpecification asMultipartUser() {
        return RestAssured.given().header(HTTPRequest.TOKEN_HEADER,
                tokenStore.open(TEST_USER_ID, TEST_USER_ID, TEST_USERNAME).token());
    }

    protected String path(String suffix) {
        return "/api/" + TEST_USER_ID + suffix;
    }

    // ── preview ────────────────────────────────────────────────────────────────

    protected ValidatableResponse preview(byte[] pdf) {
        return asMultipartUser()
                .multiPart("file", "documento.pdf", pdf, "application/pdf")
                .when().post(path(PREVIEW))
                .then();
    }

    protected ValidatableResponse preview(byte[] pdf, String password) {
        return asMultipartUser()
                .multiPart("file", "documento.pdf", pdf, "application/pdf")
                .multiPart("password", password)
                .when().post(path(PREVIEW))
                .then();
    }

    /** Preview de um texto de documento sintético; falha se o endpoint não devolver 200. */
    protected JsonPath previewOf(String documentText) throws IOException {
        return preview(PdfFixtures.withText(documentText)).statusCode(200).extract().jsonPath();
    }

    /** Preview de um fixture anonimizado ({@code /faturas/…}, {@code /extratos/…}). */
    protected JsonPath previewFixture(String resource) throws IOException {
        return preview(fixturePdf(resource)).statusCode(200).extract().jsonPath();
    }

    protected static byte[] fixturePdf(String resource) throws IOException {
        return PdfFixtures.fromResource(resource);
    }

    // ── leitura das linhas do preview ──────────────────────────────────────────

    protected static List<Map<String, Object>> rows(JsonPath json) {
        return json.getList("rows");
    }

    protected static List<Map<String, Object>> rowsWithDescription(JsonPath json, String description) {
        return rows(json).stream().filter(r -> description.equals(r.get("description"))).toList();
    }

    protected static Map<String, Object> onlyRowWithDescription(JsonPath json, String description) {
        val found = rowsWithDescription(json, description);
        assertEquals(1, found.size(), () -> "esperava uma única linha \"" + description + "\", veio " + found.size());
        return found.getFirst();
    }

    protected static List<Map<String, Object>> rowsWithLast4(JsonPath json, String last4) {
        return rows(json).stream().filter(r -> last4.equals(r.get("last4"))).toList();
    }

    protected static List<Integer> ints(List<Map<String, Object>> rows, String field) {
        return rows.stream().map(r -> (Integer) r.get(field)).toList();
    }

    protected static String str(Map<String, Object> row, String field) {
        return String.valueOf(row.get(field));
    }

    /** O JSON devolve o valor como número de ponto flutuante; comparações são por escala-insensível. */
    protected static BigDecimal amount(Map<String, Object> row) {
        return new BigDecimal(String.valueOf(row.get("amount")));
    }

    // ── seed via HTTP ──────────────────────────────────────────────────────────

    protected UUID seedAccount(String name) {
        val body = """
                {"name":"%s","balance":1000.00,"type":"CHECKING","color":"#007AFF","active":true,
                 "creditLimit":10000.00,"closingDay":1,"dueDay":10}
                """.formatted(name);
        return UUID.fromString(asTestUser()
                .body(body)
                .when().post(path("/accounts"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id"));
    }

    /** Fecha a competência {@code yyyy-MM}: a partir dela, para trás, nada pode ser importado. */
    protected void closePeriod(String period) {
        asTestUser()
                .body("{\"period\":\"%s\"}".formatted(period))
                .when().post(path("/accounts/closing"))
                .then().statusCode(200);
    }

    protected UUID seedTag(String name) {
        return UUID.fromString(asTestUser()
                .body("{\"name\":\"%s\",\"color\":\"#00FF00\"}".formatted(name))
                .when().post(path("/tags"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id"));
    }

    protected UUID seedCard(UUID accountId, String last4) {
        return UUID.fromString(asTestUser()
                .body("{\"last4\":\"%s\"}".formatted(last4))
                .when().post(path("/accounts/" + accountId + "/cards"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id"));
    }

    /**
     * Transação só aceita subcategoria — a macro existe apenas para pendurar a folha. O nome é único
     * por chamada: o mesmo teste pode precisar de mais de uma, e nome repetido é rejeitado.
     */
    protected UUID seedLeafCategory() {
        val suffix = Long.toString(CATEGORY_SEQUENCE.incrementAndGet());
        val macroId = asTestUser()
                .body("{\"name\":\"Macro %s\",\"nature\":\"EXPENSE\"}".formatted(suffix))
                .when().post(path("/categories"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        val leafId = asTestUser()
                .body("{\"name\":\"Folha %s\",\"nature\":\"EXPENSE\",\"parentId\":\"%s\"}".formatted(suffix, macroId))
                .when().post(path("/categories"))
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return UUID.fromString(leafId);
    }

    private static final AtomicLong CATEGORY_SEQUENCE = new AtomicLong();
}
