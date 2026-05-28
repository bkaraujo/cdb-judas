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

        String response = mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Conta Corrente"))
                .andExpect(jsonPath("$.balance").value(1250.50))
                .andReturn().getResponse().getContentAsString();

        Account created = objectMapper.readValue(response, Account.class);
        UUID id = created.id();

        mockMvc.perform(get("/api/{u}/accounts/{id}", TEST_USER_ID, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.additionalInfo.bank").value("Banco do Brasil"));

        mockMvc.perform(get("/api/{u}/accounts", TEST_USER_ID))
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

        mockMvc.perform(patch("/api/{u}/accounts/{id}", TEST_USER_ID, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Conta Alterada"))
                .andExpect(jsonPath("$.balance").value(1300.00));

        mockMvc.perform(get("/api/{u}/accounts/{id}/balance", TEST_USER_ID, id)
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/{u}/accounts/{id}/balance", TEST_USER_ID, id))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/{u}/accounts/{id}", TEST_USER_ID, id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404ParaContaInexistente() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/{u}/accounts/{id}", TEST_USER_ID, missing))
                .andExpect(status().isNotFound());

        String patchJson = """
            {"name":"X","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        mockMvc.perform(patch("/api/{u}/accounts/{id}", TEST_USER_ID, missing)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/{u}/accounts/{id}", TEST_USER_ID, missing))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveGerenciarCartaoViaContas() throws Exception {
        // Conta vinculada (CHECKING) — cartão exige mesma cor da conta de origem.
        String checkingJson = """
            {"name":"Conta Corrente","balance":0.00,"type":"CHECKING","color":"#820AD1","active":true}
            """;
        String checkingResp = mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(checkingJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID checkingId = objectMapper.readValue(checkingResp, Account.class).id();

        String cardJson = """
            {
              "name":"Nubank","balance":0.00,"type":"CREDIT_CARD","color":"#820AD1","active":true,
              "linkedAccountId":"%s",
              "additionalInfo":{"last4":"1234","limit":5000.00,"closingDay":5,"dueDay":12}
            }
            """.formatted(checkingId);
        String cardResp = mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(cardJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.linkedAccountId").value(checkingId.toString()))
                .andExpect(jsonPath("$.additionalInfo.last4").value("1234"))
                .andReturn().getResponse().getContentAsString();
        UUID cardId = objectMapper.readValue(cardResp, Account.class).id();

        // Listar apenas cartões via filtro de tipo.
        mockMvc.perform(get("/api/{u}/accounts", TEST_USER_ID).param("type", "card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$[0].type").value("CREDIT_CARD"));

        // Editar o cartão pelos endpoints de item de conta.
        String patchCard = """
            {
              "name":"Nubank","balance":0.00,"type":"CREDIT_CARD","color":"#820AD1","active":true,
              "linkedAccountId":"%s",
              "additionalInfo":{"last4":"9999","limit":8000.00,"closingDay":5,"dueDay":12}
            }
            """.formatted(checkingId);
        mockMvc.perform(patch("/api/{u}/accounts/{id}", TEST_USER_ID, cardId)
                .contentType(MediaType.APPLICATION_JSON).content(patchCard))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalInfo.last4").value("9999"));

        // Excluir o cartão.
        mockMvc.perform(delete("/api/{u}/accounts/{id}", TEST_USER_ID, cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/{u}/accounts", TEST_USER_ID).param("type", "card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
