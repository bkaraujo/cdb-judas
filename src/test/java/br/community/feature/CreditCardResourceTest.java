package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditCardResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarCartoes() throws Exception {
        // Criar conta corrente para vincular o cartão
        String accJson = """
            {
              "name": "Conta Corrente",
              "balance": 1000.00,
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true
            }
            """;
        String accResponse = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                .andReturn().getResponse().getContentAsString();
        UUID accountId = UUID.fromString(objectMapper.readTree(accResponse).get("id").asText());

        // Testar falha com cor diferente
        String invalidColorJson = """
            {
              "accountId": "%s",
              "name": "Black Card",
              "last4": "4321",
              "limit": 10000.00,
              "closingDay": 1,
              "dueDay": 10,
              "color": "#333333",
              "active": true
            }
            """.formatted(accountId);

        mockMvc.perform(post("/api/credit-cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidColorJson))
                .andExpect(status().isBadRequest());

        // Testar sucesso com cor igual
        String createJson = """
            {
              "accountId": "%s",
              "name": "Black Card",
              "last4": "4321",
              "limit": 10000.00,
              "closingDay": 1,
              "dueDay": 10,
              "color": "#007AFF",
              "active": true
            }
            """.formatted(accountId);

        mockMvc.perform(post("/api/credit-cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.last4").value("4321"))
                .andExpect(jsonPath("$.limit").value(10000.00));

        String listResp = mockMvc.perform(get("/api/credit-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID cardId = UUID.fromString(objectMapper.readTree(listResp).get(0).get("id").asText());

        String patchJson = """
            {
              "accountId": "%s",
              "name": "Platinum Card",
              "last4": "9876",
              "limit": 20000.00,
              "closingDay": 5,
              "dueDay": 15,
              "color": "#007AFF",
              "active": true
            }
            """.formatted(accountId);

        mockMvc.perform(patch("/api/credit-cards/{id}", cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last4").value("9876"))
                .andExpect(jsonPath("$.limit").value(20000.00))
                .andExpect(jsonPath("$.closingDay").value(5))
                .andExpect(jsonPath("$.dueDay").value(15));

        String patchInvalidColor = """
            {
              "accountId": "%s",
              "name": "Platinum Card",
              "last4": "9876",
              "limit": 20000.00,
              "closingDay": 5,
              "dueDay": 15,
              "color": "#FF0000",
              "active": true
            }
            """.formatted(accountId);

        mockMvc.perform(patch("/api/credit-cards/{id}", cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchInvalidColor))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/credit-cards/{id}", cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/credit-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
