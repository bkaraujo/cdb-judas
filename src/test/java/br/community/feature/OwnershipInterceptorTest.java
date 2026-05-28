package br.community.feature;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnershipInterceptorTest extends BaseHttpTest {

    @Test
    void permiteQuandoUuidDaRotaIgualAoAutenticado() throws Exception {
        mockMvc.perform(get("/api/{u}/accounts", TEST_USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void retorna403QuandoUuidDivergente() throws Exception {
        String outroUsuario = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/{u}/accounts", outroUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    void retorna403QuandoUuidMalformado() throws Exception {
        mockMvc.perform(get("/api/{u}/accounts", "nao-e-uuid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoGuardaRotaGlobalDeCentroDeCusto() throws Exception {
        // /api/cost-centers é global (isenta da guarda); jamais retorna 403 por propriedade.
        mockMvc.perform(get("/api/cost-centers"))
                .andExpect(status().isOk());
    }
}
