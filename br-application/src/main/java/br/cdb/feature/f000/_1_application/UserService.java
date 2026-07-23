package br.cdb.feature.f000._1_application;

import br.cdb.core.security.User;
import br.cdb.core.security.UserRepository;
import br.cdb.feature.f000._0_domain.event.UserEvents;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@Singleton
@NullMarked
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Result<User, BusinessError> createUser(String username, String name, char[] password) {
        Logger.debug("Criando usuário %s", username);

        var user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            return Result.failure(new BusinessError.Validation("Usuário [%s] já existe".formatted(username)));
        }

        user = userRepository.save(new User(
                UUID.randomUUID().toString(),
                username,
                name,
                BcryptUtil.bcryptHash(new String(password))
        ));

        MessageBus.submit(new UserEvents.Created(user.id(), user.username()));
        return Result.success(user);
    }

}
