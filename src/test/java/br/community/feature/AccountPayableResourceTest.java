package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountPayableResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarContasAPagar() throws Exception {
        // Criar uma conta e uma transação pendente
        String accJson = """
            {
              "name": "Banco",
              "balance": 0.00,
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
              "name": "Serviços",
              "nature": "EXPENSE"
            }
            """;
        String catResp = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                .andReturn().getResponse().getContentAsString();
        UUID categoryId = UUID.fromString(objectMapper.readTree(catResp).get("id").asText());

        String transJson = """
            {
              "description": "Internet",
              "amount": -120.00,
              "date": "2024-03-20",
              "categoryId": "%s",
              "accountId": "%s",
              "status": "pending",
              "type": "expense"
            }
            """.formatted(categoryId, accountId);
        String transResp = mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(transJson))
                .andReturn().getResponse().getContentAsString();
        UUID transId = UUID.fromString(objectMapper.readTree(transResp).get("id").asText());

        // Listar pendentes
        mockMvc.perform(get("/api/operations/payables").param("type", "PAYABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Internet"))
                .andExpect(jsonPath("$[0].status").value("pending"));

        // Confirmar pagamento
        String confirmJson = """
            {
              "paymentDate": "2024-03-19"
            }
            """;
        mockMvc.perform(put("/api/operations/payables/{id}/confirm", transId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"));
    }

    @Test
    void deveListarRecebiveis() throws Exception {
        String accJson = """
            {"name":"Conta R","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String catJson = """
            {"name":"Salário","nature":"REVENUE"}
            """;
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String incomeJson = """
            {"description":"Freelance","amount":1500.00,"date":"2024-04-01","categoryId":"%s","accountId":"%s","status":"pending","type":"income"}
            """.formatted(categoryId, accountId);
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(incomeJson))
                .andExpect(status().isCreated());

        String expenseJson = """
            {"description":"Conta luz","amount":-80.00,"date":"2024-04-02","categoryId":"%s","accountId":"%s","status":"pending","type":"expense"}
            """.formatted(categoryId, accountId);
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(expenseJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/operations/payables").param("type", "RECEIVABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Freelance"))
                .andExpect(jsonPath("$[0].type").value("RECEIVABLE"));

        mockMvc.perform(get("/api/operations/payables").param("type", "PAYABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Conta luz"));
    }
}
