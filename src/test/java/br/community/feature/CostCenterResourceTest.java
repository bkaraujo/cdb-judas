package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CostCenterResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarCentrosDeCusto() throws Exception {
        String createJson = """
            {
              "description": "Trabalho"
            }
            """;

        String createResp = mockMvc.perform(post("/api/cost-centers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Trabalho"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(createResp).get("id").asText());

        mockMvc.perform(get("/api/cost-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Trabalho"));

        String patchJson = """
            {
              "description": "Pessoal"
            }
            """;

        mockMvc.perform(patch("/api/cost-centers/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Pessoal"));

        mockMvc.perform(delete("/api/cost-centers/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cost-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
