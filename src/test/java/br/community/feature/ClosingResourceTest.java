package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClosingResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarFechamento() throws Exception {
        String closingJson = """
            {
              "period": "2024-02"
            }
            """;

        mockMvc.perform(post("/api/operations/closing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(closingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2024-02"));

        mockMvc.perform(get("/api/operations/closing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2024-02"));

        mockMvc.perform(delete("/api/operations/closing"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/operations/closing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").doesNotExist());
    }

    @Test
    void deveBloquearLancamentoEmPeriodoFechado() throws Exception {
        String accJson = """
            {"name":"Conta F","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        String catJson = """
            {"name":"Geral","nature":"EXPENSE"}
            """;
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(catJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/operations/closing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"period":"2024-06"}
                    """))
                .andExpect(status().isOk());

        String blockedJson = """
            {"description":"Dentro do fechado","amount":-50.00,"date":"2024-06-15","categoryId":"%s","accountId":"%s","status":"pending","type":"expense"}
            """.formatted(categoryId, accountId);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockedJson))
                .andExpect(status().isBadRequest());

        String okJson = """
            {"description":"Pos fechamento","amount":-50.00,"date":"2024-07-15","categoryId":"%s","accountId":"%s","status":"pending","type":"expense"}
            """.formatted(categoryId, accountId);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(okJson))
                .andExpect(status().isCreated());
    }
}
