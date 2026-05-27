package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TagResourceTest extends BaseHttpTest {

    @Test
    void deveGerenciarTags() throws Exception {
        String createJson = """
            {
              "name": "Viagem",
              "color": "#00FF00"
            }
            """;

        String createResp = mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Viagem"))
                .andReturn().getResponse().getContentAsString();
        UUID tagId = UUID.fromString(objectMapper.readTree(createResp).get("id").asText());

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Viagem"));

        String patchJson = """
            {
              "name": "Lazer",
              "color": "#0000FF"
            }
            """;

        mockMvc.perform(patch("/api/tags/{id}", tagId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lazer"))
                .andExpect(jsonPath("$.color").value("#0000FF"));

        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
