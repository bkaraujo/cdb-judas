package br.community.feature;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementResourceTest extends BaseHttpTest {

    private UUID createAccount(double balance, String color) throws Exception {
        String json = """
            {"name":"Conta","balance":%s,"type":"CHECKING","color":"%s","active":true}
            """.formatted(balance, color);
        String resp = mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    private UUID createLeafCategory() throws Exception {
        String macro = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Geral\",\"nature\":\"EXPENSE\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID macroId = UUID.fromString(objectMapper.readTree(macro).get("id").asText());
        String sub = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sub\",\"nature\":\"EXPENSE\",\"parentId\":\"" + macroId + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(sub).get("id").asText());
    }

    private void createTx(UUID accId, UUID catId, String date, String amount, String status, String type) throws Exception {
        String json = """
            {"description":"Mov","amount":%s,"date":"%s","categoryId":"%s","status":"%s","type":"%s","installments":1,"editMode":"single"}
            """.formatted(amount, date, catId, status, type);
        mockMvc.perform(post("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accId)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    private JsonNode summaryFor(String yyyyMM, String accId, String statusParam) throws Exception {
        var req = get("/api/{u}/accounts/statements/{ym}", TEST_USER_ID, yyyyMM);
        if (statusParam != null) req = req.param("status", statusParam);
        String resp = mockMvc.perform(req).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(resp)) {
            if (accId.equals(node.get("accountId").asText())) return node;
        }
        return null;
    }

    @Test
    void deveObterExtratoDetalhadoComSaldoCorrente() throws Exception {
        UUID acc = createAccount(1000.00, "#000000");
        UUID cat = createLeafCategory();
        createTx(acc, cat, "2024-03-05", "-250.00", "confirmed", "expense");

        mockMvc.perform(get("/api/{u}/accounts/{acc}/statements/{ym}", TEST_USER_ID, acc, "202403"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Previous balance"))
                .andExpect(jsonPath("$[0].runningBal").value(1000.00))
                .andExpect(jsonPath("$[1].description").value("Mov"))
                .andExpect(jsonPath("$[1].runningBal").value(750.00));
    }

    @Test
    void resumoCalculaSaldosETotaisPorConta() throws Exception {
        UUID acc = createAccount(1000.00, "#000000");
        UUID cat = createLeafCategory();
        createTx(acc, cat, "2024-03-05", "-250.00", "confirmed", "expense");
        createTx(acc, cat, "2024-03-10", "100.00", "confirmed", "income");

        JsonNode s = summaryFor("202403", acc.toString(), null);
        assertNotNull(s);
        assertEquals(1000.00, s.get("openingBalance").asDouble(), 0.001);
        assertEquals(100.00, s.get("totalIn").asDouble(), 0.001);
        assertEquals(250.00, s.get("totalOut").asDouble(), 0.001);
        assertEquals(850.00, s.get("closingBalance").asDouble(), 0.001);
    }

    @Test
    void resumoMesVazioTemInicialIgualAoFinal() throws Exception {
        UUID acc = createAccount(500.00, "#000000");

        JsonNode s = summaryFor("202408", acc.toString(), null);
        assertNotNull(s);
        assertEquals(500.00, s.get("openingBalance").asDouble(), 0.001);
        assertEquals(500.00, s.get("closingBalance").asDouble(), 0.001);
        assertEquals(0.00, s.get("totalIn").asDouble(), 0.001);
        assertEquals(0.00, s.get("totalOut").asDouble(), 0.001);
    }

    @Test
    void resumoConsideraTransferencias() throws Exception {
        UUID origem = createAccount(1000.00, "#010101");
        UUID destino = createAccount(1000.00, "#020202");

        String transfer = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-03-15","amount":150.00}
            """.formatted(origem, destino);
        mockMvc.perform(post("/api/{u}/accounts/transactions/transfer", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(transfer))
                .andExpect(status().isCreated());

        JsonNode out = summaryFor("202403", origem.toString(), null);
        assertNotNull(out);
        assertEquals(150.00, out.get("totalOut").asDouble(), 0.001);
        assertEquals(850.00, out.get("closingBalance").asDouble(), 0.001);

        JsonNode in = summaryFor("202403", destino.toString(), null);
        assertNotNull(in);
        assertEquals(150.00, in.get("totalIn").asDouble(), 0.001);
        assertEquals(1150.00, in.get("closingBalance").asDouble(), 0.001);
    }

    @Test
    void resumoFiltraPorStatus() throws Exception {
        UUID acc = createAccount(1000.00, "#000000");
        UUID cat = createLeafCategory();
        createTx(acc, cat, "2024-03-05", "-250.00", "confirmed", "expense");
        createTx(acc, cat, "2024-03-06", "-100.00", "pending", "expense");

        JsonNode confirmed = summaryFor("202403", acc.toString(), "confirmed");
        assertNotNull(confirmed);
        assertEquals(250.00, confirmed.get("totalOut").asDouble(), 0.001);
        assertEquals(750.00, confirmed.get("closingBalance").asDouble(), 0.001);
    }
}
