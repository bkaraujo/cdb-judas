package br.cdb.feature.system.user;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Semeia o usuário {@code admin}, a fonte global de centros de custo e as categorias padrões
 * no startup. A ordem em relação ao schema é garantida pelo observer {@code @Priority(1)}
 * de {@code ContextBridge} (DataSource antes).
 */
@Singleton
@NullMarked
@RequiredArgsConstructor
public final class UserSeeder {

    private final UserService userService;

    void seed(@Observes StartupEvent event) {
        userService.createUser("admin", "", "admin".toCharArray());
    }
}