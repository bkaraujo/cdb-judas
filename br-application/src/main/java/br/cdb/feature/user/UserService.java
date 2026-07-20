package br.cdb.feature.user;

import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.cdb.feature.user.seed.UserProvisioningStep;
import br.commons.Logger;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
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
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            val id = UUID.randomUUID().toString();
            userRepository.save(new User(
                    id,
                    username,
                    name,
                    BcryptUtil.bcryptHash(new String(password))
            ));
            Logger.info("usuário '%s' criado com id %s", username, id);
            // Re-lê para obter o personId cunhado pelo repositório ao persistir.
            user = userRepository.findByUsername(username).orElseThrow();
        }

        // O provisionamento (categorias etc.) faz chave com a PESSOA, não com o login.
        val personId = UUID.fromString(Objects.requireNonNull(user.personId(), "usuário sem pessoa vinculada"));
        provisioningSteps.forEach(step -> step.provision(personId));
    }

}
