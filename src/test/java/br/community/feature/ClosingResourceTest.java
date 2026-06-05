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
        mockMvc.perform(post("/api/{u}/accounts/closing", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"2024-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2024-02"));

        mockMvc.perform(get("/api/{u}/accounts/closing", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2024-02"));

        mockMvc.perform(delete("/api/{u}/accounts/closing", TEST_USER_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/{u}/accounts/closing", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").doesNotExist());
    }

    @Test
    void deveBloquearLancamentoEmPeriodoFechado() throws Exception {
        String accJson = """
            {"name":"Conta F","balance":0.00,"type":"CHECKING","color":"#000000","active":true}
            """;
        UUID accountId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID).contentType(MediaType.APPLICATION_JSON).content(accJson))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID macroId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Geral\",\"nature\":\"EXPENSE\"}"))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sub\",\"nature\":\"EXPENSE\",\"parentId\":\"" + macroId + "\"}"))
                        .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/{u}/accounts/closing", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"2024-06\"}"))
                .andExpect(status().isOk());

        String blockedJson = """
            {"description":"Dentro do fechado","amount":-50.00,"date":"2024-06-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        mockMvc.perform(post("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accountId)
                .contentType(MediaType.APPLICATION_JSON).content(blockedJson))
                .andExpect(status().isBadRequest());

        String okJson = """
            {"description":"Pos fechamento","amount":-50.00,"date":"2024-07-15","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense"}
            """.formatted(categoryId);
        mockMvc.perform(post("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accountId)
                .contentType(MediaType.APPLICATION_JSON).content(okJson))
                .andExpect(status().isCreated());
    }
}
