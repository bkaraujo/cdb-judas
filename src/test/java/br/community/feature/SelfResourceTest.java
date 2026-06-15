package br.community.feature;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.Repository;
import br.community.context.security._0_domain.model.Preferences;
import br.community.context.security._0_domain.model.User;
import br.community.context.security._0_domain.repository.UserRepository;
import br.community.core.JsonStorageProperties;
import br.community.core.web.filter.AuthenticationFilter;
import br.community.core.web.filter.AuthorizationFilter;
import br.community.core.web.security.AccessTokenStore;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static br.community.feature.system.auth.LoginResource.TOKEN_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração HTTP do recurso self. Exercita a cadeia real de filtros (autenticação por
 * token + autorização) para validar 200 autenticado e 401 sem token — prior art:
 * AccountResourceTest sobre BaseHttpTest, aqui com os filtros reais montados.
 */
@SpringBootTest
@ActiveProfiles("test")
class SelfResourceTest {

    private static final String USER_ID = "00000000-0000-0000-0000-00000000beef";

    @Autowired WebApplicationContext context;
    @Autowired JsonStorageProperties storageProperties;
    @Autowired List<Repository<?, ?>> repositories;
    @Autowired Storage storage;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccessTokenStore tokenStore;
    @Autowired UserRepository userRepository;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws IOException {
        val path = Path.of(storageProperties.path());
        FileSystemUtils.deleteRecursively(path);
        new File(path.toString()).mkdirs();
        repositories.forEach(Repository::clearCache);
        SecurityContextHolder.clearContext();

        userRepository.save(new User(USER_ID, "tester", "Tester", "hash", Preferences.defaults()));
        token = tokenStore.issue(USER_ID);

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new AuthenticationFilter(tokenStore, userRepository), new AuthorizationFilter())
                .build();
    }

    @Test
    void getMeAutenticadoRetorna200ComPerfil() throws Exception {
        mockMvc.perform(get("/api/me").header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.name").value("Tester"))
                .andExpect(jsonPath("$.preferences.theme").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.preferences.language").value("pt-BR"))
                .andExpect(jsonPath("$.preferences.locale").value("pt-BR"))
                .andExpect(jsonPath("$.preferences.sidebarCollapsed").value(false));
    }

    @Test
    void getMeSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchSomenteNomeNaoAfetaPreferencias() throws Exception {
        userRepository.save(new User(USER_ID, "tester", "Tester", "hash",
                new Preferences("dark", "pt-BR", "pt-BR", false)));

        mockMvc.perform(patch("/api/me").header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renomeado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renomeado"))
                .andExpect(jsonPath("$.preferences.theme").value("dark"))
                .andExpect(jsonPath("$.preferences.sidebarCollapsed").value(false));
    }

    @Test
    void patchSomentePreferenciasNaoAfetaNome() throws Exception {
        userRepository.save(new User(USER_ID, "tester", "Mantido", "hash", Preferences.defaults()));

        mockMvc.perform(patch("/api/me").header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferences\":{\"theme\":\"light\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mantido"))
                .andExpect(jsonPath("$.preferences.theme").value("light"))
                .andExpect(jsonPath("$.preferences.language").value("pt-BR"));
    }

    @Test
    void patchSemTokenRetorna401() throws Exception {
        mockMvc.perform(patch("/api/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
