package br.cdb.feature.f000._1_application.service;

import br.cdb.core.persistence.UserRepository;
import br.cdb.core.security.User;
import br.cdb.feature.f000._0_domain.event.UserEvents;
import br.cdb.feature.f000._1_application.usecase.WriteUseCase;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@Singleton
@NullMarked
@RequiredArgsConstructor
public class UserService {

    // O par de use cases não é bean CDI — resolve-se pelo Context (mesmo padrão de f001 ProfileService).
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);
    private final UserRepository userRepository = Context.get(UserRepository.class);

    public Result<User, BusinessError> createUser(String username, String name, char[] password) {
        Logger.debug("Criando usuário %s", username);

        if (userRepository.findByUsername(username).isPresent()) {
            return Result.failure(new BusinessError.Validation("Usuário [%s] já existe".formatted(username)));
        }

        // 1 - Cria a Person (dono dos recursos; toda a funcionalidade referencia a Person)
        return writes.registerPerson(name, UserGuards.DEFAULT_LOCALE, UserGuards.DEFAULT_LANGUAGE).map(person -> {
            // 2 - Cria o User (login) vinculado à Person
            val user = userRepository.save(new User(
                    UUID.randomUUID().toString(),
                    username,
                    name,
                    BcryptUtil.bcryptHash(new String(password)),
                    true, null, null,
                    person.id().toString()
            ));

            MessageBus.submit(new UserEvents.Created(user.id(), person.id().toString(), user.username()));
            return user;
        });
    }

}
