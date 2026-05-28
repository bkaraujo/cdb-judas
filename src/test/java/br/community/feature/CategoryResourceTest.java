package br.community.feature;

import br.community.context.monetary._0_domain.model.MonetaryNature;
import br.community.feature.records.categories.CategoryDTO;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryResourceTest extends BaseHttpTest {

    @Test
    void deveCriarEListarCategorys() throws Exception {
        val createJson = """
            {
              "name": "Alimentação",
              "nature": "EXPENSE"
            }
            """;

        val createResult = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson));

        createResult.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alimentação"))
                .andExpect(jsonPath("$.nature").value(MonetaryNature.EXPENSE.name()));

        val responseBody = createResult.andReturn().getResponse().getContentAsString();
        val created = objectMapper.readValue(responseBody, CategoryDTO.class);
        val id = created.id();

        mockMvc.perform(get("/api/" + TEST_USER_ID + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Alimentação"));

        val updateJson = """
            {
              "name": "Supermercado"
            }
            """;

        mockMvc.perform(patch("/api/" + TEST_USER_ID + "/categories/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Supermercado"));

        mockMvc.perform(delete("/api/" + TEST_USER_ID + "/categories/{id}", id))
                .andExpect(status().isNoContent());

        // After deletion, "Outros" fallback category is auto-created
        mockMvc.perform(get("/api/" + TEST_USER_ID + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Outros"));
    }

    @Test
    void shouldRejectSubcategoryOfSubcategory() throws Exception {
        val root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        val rootBody = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON).content(root))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        val rootId = objectMapper.readTree(rootBody).get("id").asText();

        val sub = """
            {"name": "Aluguel", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(rootId);
        val subBody = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON).content(sub))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        val subId = objectMapper.readTree(subBody).get("id").asText();

        val subsub = """
            {"name": "IPTU", "nature": "EXPENSE", "parentId": "%s"}
            """.formatted(subId);
        mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON).content(subsub))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectSubcategoryWithDifferentNature() throws Exception {
        val root = """
            {"name": "Moradia", "nature": "EXPENSE"}
            """;
        val rootBody = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON).content(root))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        val rootId = objectMapper.readTree(rootBody).get("id").asText();

        val sub = """
            {"name": "Salário", "nature": "REVENUE", "parentId": "%s"}
            """.formatted(rootId);
        mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories").contentType(MediaType.APPLICATION_JSON).content(sub))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateCategoryAndSubcategory() throws Exception {
        val moradiaJson = """
            {
              "name": "Moradia",
              "nature": "EXPENSE"
            }
            """;

        val createResult = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moradiaJson));

        createResult.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Moradia"))
                .andExpect(jsonPath("$.nature").value(MonetaryNature.EXPENSE.name()));

        mockMvc.perform(get("/api/" + TEST_USER_ID + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        val moradiaResponseBody = createResult.andReturn().getResponse().getContentAsString();
        val moradiaCreated = objectMapper.readValue(moradiaResponseBody, CategoryDTO.class);
        val moradiaId = moradiaCreated.id();

        val aluguelJson = """
            {
              "name": "Aluguél",
              "nature": "EXPENSE",
              "parentId": "%s"
            }
            """.formatted(moradiaId.toString());

        val aluguelResult = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aluguelJson));

        aluguelResult.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Aluguél"))
                .andExpect(jsonPath("$.parentId").value(moradiaId.toString()))
                .andExpect(jsonPath("$.nature").value(MonetaryNature.EXPENSE.name()));

        mockMvc.perform(get("/api/" + TEST_USER_ID + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/" + TEST_USER_ID + "/categories/{id}", moradiaId.toString()))
                .andExpect(status().isNoContent());

        // After deletion, "Outros" fallback category is auto-created
        mockMvc.perform(get("/api/" + TEST_USER_ID + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Outros"));

    }

}
