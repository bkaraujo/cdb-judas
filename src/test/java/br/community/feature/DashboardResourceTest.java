package br.community.feature;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardResourceTest extends BaseHttpTest {

    @Test
    void deveObterResultadoMensal() throws Exception {
        mockMvc.perform(get("/api/dashboard/result")
                .param("month", "3")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes").exists())
                .andExpect(jsonPath("$.expenses").exists())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.history").isArray());
    }
}
