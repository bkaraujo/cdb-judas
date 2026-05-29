package br.community.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CostCenterResourceTest extends BaseHttpTest {

    private static final String CATALOG = """
            [ {"id":"d0000000-0000-0000-0000-000000000001","description":"Fixo"},
              {"id":"d0000000-0000-0000-0000-000000000002","description":"Variável"} ]""";

    @Test
    void retornaListaFixaGlobalSomenteLeitura() throws Exception {
        storage.write("cost-centers.json", "costCenters", CATALOG.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/cost-center"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("d0000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[0].description").value("Fixo"))
                .andExpect(jsonPath("$[1].description").value("Variável"));
    }

    @Test
    void naoPermiteCriacaoViaApi() throws Exception {
        mockMvc.perform(post("/api/cost-center")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Trabalho\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void naoPermiteExclusaoViaApi() throws Exception {
        mockMvc.perform(delete("/api/cost-center"))
                .andExpect(status().isMethodNotAllowed());
    }
}
