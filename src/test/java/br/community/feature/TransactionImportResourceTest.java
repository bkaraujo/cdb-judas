package br.community.feature;

import br.community.PdfFixtures;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionImportResourceTest extends BaseHttpTest {

    @Test
    void previewDetectsBtgIssuer() throws Exception {
        byte[] pdf = PdfFixtures.withText("BTG Pactual S.A\nCNPJ 30.306.294/0001-45\ncartao final 5115");
        var file = new MockMultipartFile("file", "fatura.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/transactions/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("BTG"));
    }

    @Test
    void previewReturnsParsedRowsAndLast4() throws Exception {
        byte[] pdf = PdfFixtures.withText(String.join("\n",
                "BTG Pactual S.A",
                "CNPJ 30.306.294/0001-45",
                "Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99",
                "Total de compras e despesas",
                "R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul",
                "R$ 72,99"));
        var file = new MockMultipartFile("file", "fatura.pdf", "application/pdf", pdf);

        // The single printed "(9/10)" line expands into the full 10-installment schedule.
        mockMvc.perform(multipart("/api/transactions/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("BTG"))
                .andExpect(jsonPath("$.last4s[0]").value("0020"))
                .andExpect(jsonPath("$.rows.length()").value(10))
                .andExpect(jsonPath("$.rows[0].description").value("Amazonmktplc Megabytem"))
                .andExpect(jsonPath("$.rows[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.rows[0].installmentTotal").value(10))
                .andExpect(jsonPath("$.rows[9].installmentNumber").value(10))
                // Month/day of the first installment is stable ("-07-15"); the year depends on the
                // real system clock in this Spring test, so it is intentionally not asserted.
                .andExpect(jsonPath("$.rows[0].date").value(Matchers.endsWith("-07-15")));
    }

    @Test
    void previewEncryptedWithoutPasswordReturns422WithPasswordCode() throws Exception {
        byte[] pdf = PdfFixtures.encrypted("segredo", "BTG Pactual 30.306.294/0001-45");
        var file = new MockMultipartFile("file", "fatura.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/transactions/import/preview").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASSWORD_REQUIRED"));
    }

    @Test
    void previewEncryptedWithWrongPasswordReturns422WithWrongPasswordCode() throws Exception {
        byte[] pdf = PdfFixtures.encrypted("segredo", "BTG Pactual 30.306.294/0001-45");
        var file = new MockMultipartFile("file", "fatura.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/transactions/import/preview").file(file).param("password", "errada"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
    }

    @Test
    void previewUnknownBankReturns422WithClearMessage() throws Exception {
        byte[] pdf = PdfFixtures.withText("Documento qualquer sem marcador de banco");
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/transactions/import/preview").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_ISSUER"))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("não reconhecido")));
    }

    @Test
    void confirmPersistsRowsAndIsIdempotentOnReimport() throws Exception {
        UUID cardId = seedCheckingAndCard();
        UUID categoryId = UUID.randomUUID();

        String body = """
            {
              "cardId": "%s",
              "rows": [
                {"description":"Mercado","amount":80.00,"date":"2025-07-10","originalDate":"2025-07-10","categoryId":"%s"},
                {"description":"Geladeira","amount":100.00,"date":"2025-07-15","originalDate":"2025-07-15","installmentNumber":1,"installmentTotal":2,"categoryId":"%s"},
                {"description":"Geladeira","amount":100.00,"date":"2025-08-15","originalDate":"2025-07-15","installmentNumber":2,"installmentTotal":2,"categoryId":"%s"}
              ]
            }
            """.formatted(cardId, categoryId, categoryId, categoryId);

        mockMvc.perform(post("/api/transactions/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(3))
                .andExpect(jsonPath("$.skipped").value(0));

        // A second identical POST creates nothing (idempotent re-import).
        mockMvc.perform(post("/api/transactions/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(3));
    }

    @Test
    void confirmWithUnknownCardReturns422CardNotFound() throws Exception {
        UUID categoryId = UUID.randomUUID();
        String body = """
            {
              "cardId": "%s",
              "rows": [
                {"description":"Mercado","amount":80.00,"date":"2025-07-10","originalDate":"2025-07-10","categoryId":"%s"}
              ]
            }
            """.formatted(UUID.randomUUID(), categoryId);

        mockMvc.perform(post("/api/transactions/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
    }

    private UUID seedCheckingAndCard() throws Exception {
        String accJson = """
            {"name":"Conta Corrente","balance":1000.00,"type":"CHECKING","color":"#007AFF","active":true}
            """;
        String accResponse = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(accJson))
                .andReturn().getResponse().getContentAsString();
        UUID accountId = UUID.fromString(objectMapper.readTree(accResponse).get("id").asText());

        String cardJson = """
            {"accountId":"%s","name":"Black Card","last4":"0020","limit":10000.00,"closingDay":1,"dueDay":10,"color":"#007AFF","active":true}
            """.formatted(accountId);
        String cardResponse = mockMvc.perform(post("/api/credit-cards")
                        .contentType(MediaType.APPLICATION_JSON).content(cardJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(cardResponse).get("id").asText());
    }
}
