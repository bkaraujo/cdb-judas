package br.community.core.web.security;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Semeia o usuário {@code admin} e a fonte global de centros de custo no startup. A ordem em relação
 * ao schema é garantida pelo observer {@code @Priority(1)} de {@code ContextBridge} (DataSource antes).
 */
@Singleton
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
    private final Storage storage;

    void seed(@Observes StartupEvent event) {
        if (repository.findByUsername("admin").isEmpty()) {
            val id = UUID.randomUUID().toString();
            repository.save(new User(
                    id,
                    "admin",
                    null,
                    BcryptUtil.bcryptHash("admin")
            ));
            Logger.info("Seed => usuário 'admin' criado com id %s", id);
        }

        if (!storage.exists(COST_CENTERS_FILE)) {
            storage.write(COST_CENTERS_FILE, COST_CENTERS_KEY, COST_CENTERS_JSON.getBytes(StandardCharsets.UTF_8));
            Logger.info("Seed => fonte global de centros de custo criada (%s)", COST_CENTERS_FILE);
        }
    }
}
