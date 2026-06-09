package br.community.core.web.security;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.commons.tools.Strings;
import br.community.context.security._0_domain.repository.UserRepository;
import br.community.context.security._0_domain.model.Preferences;
import br.community.context.security._0_domain.model.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@NullMarked
@RequiredArgsConstructor
public final class UserSeeder {

    private static final String COST_CENTERS_FILE = "cost-centers.json";
    private static final String COST_CENTERS_KEY = "costCenters";
    private static final String COST_CENTERS_JSON = """
            [ {
              "id" : "d0000000-0000-0000-0000-000000000001",
              "description" : "Fixo"
            }, {
              "id" : "d0000000-0000-0000-0000-000000000002",
              "description" : "Variável"
            } ]""";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Storage storage;

    @PostConstruct
    public void seed() {
        if (repository.findByUsername("admin").isEmpty()) {
            String id = UUID.randomUUID().toString();
            repository.save(new User(
                    id,
                    "admin",
                    null,
                    Strings.orEmpty(passwordEncoder.encode("admin")),
                    Preferences.defaults()
            ));
            Logger.info("Seed => usuário 'admin' criado com id %s", id);
        }

        if (!storage.exists(COST_CENTERS_FILE)) {
            storage.write(COST_CENTERS_FILE, COST_CENTERS_KEY, COST_CENTERS_JSON.getBytes(StandardCharsets.UTF_8));
            Logger.info("Seed => fonte global de centros de custo criada (%s)", COST_CENTERS_FILE);
        }
    }
}
