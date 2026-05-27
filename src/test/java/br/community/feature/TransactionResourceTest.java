package br.community.feature;

import br.community.feature.operations.transactions.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarTransacoes() throws Exception {
        // Criar uma conta primeiro para a transação
        String accJson = """
            {
              "name": "Conta Teste",
              "balance": 1000.00,
              "type": "CHECKING",
              "color": "#000000",
              "active": true
            }
            """;
        String accResp = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                .andReturn().getResponse().getContentAsString();
        UUID accountId = UUID.fromString(objectMapper.readTree(accResp).get("id").asText());

        // Criar categoria
        String catJson = """
            {
              "name": "Moradia",
              "nature": "EXPENSE"
            }
            """;
        String catResp = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                .andReturn().getResponse().getContentAsString();
        UUID categoryId = UUID.fromString(objectMapper.readTree(catResp).get("id").asText());

        String createJson = """
            {
              "description": "Pagamento Aluguel",
              "amount": -2500.00,
              "date": "2024-04-01",
              "categoryId": "%s",
              "accountId": "%s",
              "status": "pending",
              "type": "expense",
              "installments": 1,
              "editMode": "single"
            }
            """.formatted(categoryId, accountId);

        String response = mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(-2500.00))
                .andExpect(jsonPath("$.type").value("expense"))
                .andReturn().getResponse().getContentAsString();

        Transaction created = objectMapper.readValue(response, Transaction.class);
        UUID id = created.id();

        mockMvc.perform(get("/api/transactions").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Pagamento Aluguel"));

        String statusJson = """
            {
              "status": "confirmed",
              "paymentDate": "2024-04-02"
            }
            """;

        mockMvc.perform(patch("/api/transactions/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andExpect(jsonPath("$.paymentDate").value("2024-04-02"));

        mockMvc.perform(delete("/api/transactions/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveAtualizarTransacaoCompleta() throws Exception {
        String accJson = """
            {"name":"Conta U","balance":1000.00,"type":"CHECKING","color":"#111111","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String catJson = """
            {"name":"Diversos","nature":"EXPENSE"}
            """;
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String createJson = """
            {"description":"Mercado","amount":-100.00,"date":"2024-05-10","categoryId":"%s","accountId":"%s","status":"pending","type":"expense","installments":1,"editMode":"single"}
            """.formatted(categoryId, accountId);
        UUID id = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(createJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String updateJson = """
            {"description":"Mercado Atacado","amount":-200.50,"date":"2024-05-15","categoryId":"%s","accountId":"%s","status":"confirmed","type":"expense","installments":1,"editMode":"single"}
            """.formatted(categoryId, accountId);

        mockMvc.perform(patch("/api/transactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Mercado Atacado"))
                .andExpect(jsonPath("$.amount").value(-200.50))
                .andExpect(jsonPath("$.date").value("2024-05-15"))
                .andExpect(jsonPath("$.status").value("confirmed"));
    }

    @Test
    void deveCriarParcelasESeriarExclusaoFuture() throws Exception {
        String accJson = """
            {"name":"Conta P","balance":0.00,"type":"CHECKING","color":"#222222","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String catJson = """
            {"name":"Eletro","nature":"EXPENSE"}
            """;
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String createJson = """
            {"description":"TV","amount":-300.00,"date":"2024-06-01","categoryId":"%s","accountId":"%s","status":"pending","type":"expense","installments":3,"editMode":"single"}
            """.formatted(categoryId, accountId);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").exists())
                .andExpect(jsonPath("$.installmentNumber").value(1))
                .andExpect(jsonPath("$.totalInstallments").value(3));

        String listResp = mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        var arr = objectMapper.readTree(listResp);
        UUID firstId = null;
        for (var node : arr) {
            if (node.get("installmentNumber").asInt() == 1) {
                firstId = UUID.fromString(node.get("id").asText());
                break;
            }
        }

        mockMvc.perform(delete("/api/transactions/{id}", firstId).param("mode", "FUTURE"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
