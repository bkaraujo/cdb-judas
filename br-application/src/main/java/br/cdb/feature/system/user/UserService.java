package br.cdb.feature.system.user;

import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.commons.Logger;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Cadastro/login de usuário. O provisionamento pós-criação (ex.: semear o catálogo default de
 * categorias) é delegado a cada {@link UserProvisioningStep} — evita este pacote (base do sistema)
 * importar {@code feature.user.*}; a seta de dependência aponta sempre de {@code user} para {@code system}.
 */
@Singleton
@NullMarked
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Instance<UserProvisioningStep> provisioningSteps;

    public void createUser(String username, String name, char[] password) {
        val userId = UUID.fromString(userRepository.findByUsername(username)
                .map(User::id)
                .orElseGet(() -> {
                    val id = UUID.randomUUID().toString();
                    userRepository.save(new User(
                            id,
                            username,
                            name,
                            BcryptUtil.bcryptHash(new String(password))
                    ));
                    Logger.info("usuário '%s' criado com id %s", username, id);
                    return id;
                }));

        provisioningSteps.forEach(step -> step.provision(userId));
    }

}
