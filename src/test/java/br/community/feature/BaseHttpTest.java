package br.community.feature;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.json.Repository;
import br.community.core.JsonStorageProperties;
import br.community.core.web.security.AuthenticatedUser;
import br.community.infra.persistence.Database;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

@SpringBootTest
@ActiveProfiles("test")
abstract class BaseHttpTest {

    protected static final String TEST_USER_ID = "00000000-0000-0000-0000-0000000000ad";

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Storage storage;

    @Autowired
    protected JsonStorageProperties storageProperties;

    @Autowired
    protected List<Repository<?, ?>> repositories;

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    void setUpBase() throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();

        val principal = new AuthenticatedUser(TEST_USER_ID, "admin");
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                ));

        // Clean storage directory
        val path = Path.of(storageProperties.path());
        FileSystemUtils.deleteRecursively(path);
        new File(path.toString()).mkdirs();

        // Clear repository caches
        repositories.forEach(Repository::clearCache);

        // Reset relational tables — H2 in-memory é compartilhado entre métodos de teste
        val tx = dataSource.begin().get();
        for (val sql : Database.reset()) tx.execute(sql).get();
        tx.commit().get();
    }

}
