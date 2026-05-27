package br.community.feature;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.Repository;
import br.community.core.JsonStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
abstract class BaseHttpTest {

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

    @BeforeEach
    void setUpBase() throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        
        val user = new User("admin", "password", List.of());
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                ));

        // Clean storage directory
        val path = Path.of(storageProperties.path());
        FileSystemUtils.deleteRecursively(path);
        new File(path.toString()).mkdirs();

        // Clear repository caches
        repositories.forEach(Repository::clearCache);
    }
}
