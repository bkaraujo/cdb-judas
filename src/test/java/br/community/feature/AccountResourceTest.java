package br.community.feature;

import br.community.feature.records.accounts.Account;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountResourceTest extends BaseHttpTest {

    @Test
    void deveCriarEGerenciarContas() throws Exception {
        String createJson = """
            {
              "name": "Conta Corrente",
              "balance": 1250.50,
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true,
              "linkedAccountId": null,
              "additionalInfo": {
                "bank": "Banco do Brasil"
              }
            }
            """;

        String response = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Conta Corrente"))
                .andExpect(jsonPath("$.balance").value(1250.50))
                .andReturn().getResponse().getContentAsString();

        Account created = objectMapper.readValue(response, Account.class);
        UUID id = created.id();

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.additionalInfo.bank").value("Banco do Brasil"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        String patchJson = """
            {
              "name": "Conta Alterada",
              "balance": 1300.00,
              "type": "CHECKING",
              "color": "#007AFF",
              "active": true
            }
            """;

        mockMvc.perform(patch("/api/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Conta Alterada"))
                .andExpect(jsonPath("$.balance").value(1300.00));

        mockMvc.perform(get("/api/accounts/{id}/balance", id)
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/accounts/{id}/balance", id))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/accounts/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404ParaContaInexistente() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/accounts/{id}", missing))
                .andExpect(status().isNotFound());

        String patchJson = """
            {"name":"X","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        mockMvc.perform(patch("/api/accounts/{id}", missing)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/accounts/{id}", missing))
                .andExpect(status().isNotFound());
    }
}
