package br.cdb.feature.f006;

import br.cdb.BaseHttpTest;
import br.cdb.PdfFixtures;
import br.cdb.core.web.HTTPRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class F006ImportResourceTest extends BaseHttpTest {

    /** Sem o {@code contentType(JSON)} padrão de {@link #asTestUser()}: o multipart define o seu próprio. */
    private RequestSpecification asMultipartUser() {
        return RestAssured.given().header(HTTPRequest.TOKEN_HEADER, tokenStore.persistent(TEST_USER_ID));
    }

    @Test
    void previewDetectsBtgIssuer() throws IOException {
        byte[] pdf = PdfFixtures.withText("BTG Pactual S.A\nCNPJ 30.306.294/0001-45\ncartao final 5115");

        asMultipartUser()
                .multiPart("file", "fatura.pdf", pdf, "application/pdf")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/preview")
                .then().statusCode(200)
                .body("issuer", is("BTG Pactual"));
    }

    @Test
    void previewReturnsParsedRowsAndLast4() throws IOException {
        byte[] pdf = PdfFixtures.withText(String.join("\n",
                "BTG Pactual S.A",
                "CNPJ 30.306.294/0001-45",
                "Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99",
                "Total de compras e despesas",
                "R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul",
                "R$ 72,99"));

        // The single printed "(9/10)" line expands into the full 10-installment schedule.
        asMultipartUser()
                .multiPart("file", "fatura.pdf", pdf, "application/pdf")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/preview")
                .then().statusCode(200)
                .body("issuer", is("BTG Pactual"))
                .body("last4s[0]", is("0020"))
                .body("rows.size()", is(10))
                .body("rows[0].description", is("Amazonmktplc Megabytem"))
                .body("rows[0].installmentNumber", is(1))
                .body("rows[0].installmentTotal", is(10))
                .body("rows[9].installmentNumber", is(10))
                // Month/day of the first installment is stable ("-07-15"); the year depends on the
                // real system clock, so it is intentionally not asserted.
                .body("rows[0].date", endsWith("-07-15"));
    }

    @Test
    void previewEncryptedWithoutPasswordReturns422WithPasswordCode() throws IOException {
        byte[] pdf = PdfFixtures.encrypted("segredo", "BTG Pactual 30.306.294/0001-45");

        asMultipartUser()
                .multiPart("file", "fatura.pdf", pdf, "application/pdf")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/preview")
                .then().statusCode(422)
                .body("code", is("PASSWORD_REQUIRED"));
    }

    @Test
    void previewEncryptedWithWrongPasswordReturns422WithWrongPasswordCode() throws IOException {
        byte[] pdf = PdfFixtures.encrypted("segredo", "BTG Pactual 30.306.294/0001-45");

        asMultipartUser()
                .multiPart("file", "fatura.pdf", pdf, "application/pdf")
                .multiPart("password", "errada")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/preview")
                .then().statusCode(422)
                .body("code", is("WRONG_PASSWORD"));
    }

    @Test
    void previewUnknownBankReturns422WithClearMessage() throws IOException {
        byte[] pdf = PdfFixtures.withText("Documento qualquer sem marcador de banco");

        asMultipartUser()
                .multiPart("file", "doc.pdf", pdf, "application/pdf")
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/preview")
                .then().statusCode(422)
                .body("code", is("UNKNOWN_ISSUER"))
                .body("detail", containsString("não reconhecido"));
    }

    @Test
    void confirmPersistsRowsAndIsIdempotentOnReimport() {
        Seeded seeded = seedCheckingAndCard();
        UUID cardId = seeded.cardId();
        UUID categoryId = UUID.randomUUID();

        String body = """
            {
              "type": "CREDIT_CARD_INVOICE",
              "rows": [
                {"description":"Mercado","amount":80.00,"date":"2025-07-10","originalDate":"2025-07-10","categoryId":"%s","cardId":"%s"},
                {"description":"Geladeira","amount":100.00,"date":"2025-07-15","originalDate":"2025-07-15","installmentNumber":1,"installmentTotal":2,"categoryId":"%s","cardId":"%s"},
                {"description":"Geladeira","amount":100.00,"date":"2025-08-15","originalDate":"2025-07-15","installmentNumber":2,"installmentTotal":2,"categoryId":"%s","cardId":"%s"}
              ]
            }
            """.formatted(categoryId, cardId, categoryId, cardId, categoryId, cardId);

        asTestUser()
                .body(body)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/confirm")
                .then().statusCode(200)
                .body("created", is(3))
                .body("skipped", is(0));

        // A transação é postada na conta real do cartão, com o cardId de origem preservado.
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts/transactions")
                .then().statusCode(200)
                .body("find { it.description == 'Mercado' }.accountId", is(seeded.accountId().toString()))
                .body("find { it.description == 'Mercado' }.cardId", is(cardId.toString()));

        // A second identical POST creates nothing (idempotent re-import).
        asTestUser()
                .body(body)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/confirm")
                .then().statusCode(200)
                .body("created", is(0))
                .body("skipped", is(3));
    }

    @Test
    void confirmWithUnknownCardReturns422CardNotFound() {
        UUID categoryId = UUID.randomUUID();
        String body = """
            {
              "type": "CREDIT_CARD_INVOICE",
              "rows": [
                {"description":"Mercado","amount":80.00,"date":"2025-07-10","originalDate":"2025-07-10","categoryId":"%s","cardId":"%s"}
              ]
            }
            """.formatted(categoryId, UUID.randomUUID());

        asTestUser()
                .body(body)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/confirm")
                .then().statusCode(422)
                .body("code", is("CARD_NOT_FOUND"));
    }

    private record Seeded(UUID accountId, UUID cardId) {}

    private Seeded seedCheckingAndCard() {
        String accJson = """
            {"name":"Conta Corrente","balance":1000.00,"type":"CHECKING","color":"#007AFF","active":true,
             "creditLimit":10000.00,"closingDay":1,"dueDay":10}
            """;
        String accountId = asTestUser()
                .body(accJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts")
                .then().extract().jsonPath().getString("id");

        String cardId = asTestUser()
                .body("{\"last4\":\"0020\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return new Seeded(UUID.fromString(accountId), UUID.fromString(cardId));
    }
}
