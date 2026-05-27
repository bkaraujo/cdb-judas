package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementResourceTest extends BaseHttpTest {

    @Test
    void deveObterExtrato() throws Exception {
        // Criar conta e transação para ter dados no extrato
        String accJson = """
            {
              "name": "Conta Extrato",
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
              "name": "Compras",
              "nature": "EXPENSE"
            }
            """;
        String catResp = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                .andReturn().getResponse().getContentAsString();
        UUID categoryId = UUID.fromString(objectMapper.readTree(catResp).get("id").asText());

        String transJson = """
            {
              "description": "Compra Amazon",
              "amount": -250.00,
              "date": "2024-03-05",
              "categoryId": "%s",
              "accountId": "%s",
              "status": "confirmed",
              "type": "expense"
            }
            """.formatted(categoryId, accountId);
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(transJson));

        mockMvc.perform(get("/api/statement")
                .param("accountId", accountId.toString())
                .param("month", "3")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Previous balance"))
                .andExpect(jsonPath("$[1].description").value("Compra Amazon"))
                .andExpect(jsonPath("$[1].runningBal").value(750.00));
    }

    @Test
    void deveRetornarExtratoVazioParaMesSemTransacoes() throws Exception {
        String accJson = """
            {"name":"Conta Vazia","balance":500.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/statement")
                .param("accountId", accountId.toString())
                .param("month", "8")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Previous balance"));
    }
}
