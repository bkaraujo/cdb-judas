package br.community.context.security;

import br.commons.Result;
import br.commons.annotation.Facade;
import br.community.context.security._0_domain.model.PreferencesPatch;
import br.community.context.security._0_domain.model.User;
import br.community.context.security._1_application.usecase.UserUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fachada do contexto de segurança. Atende à regra de arquitetura "feature acessa
 * contexto apenas via Facade". Prior art: {@code MonetaryContext}.
 */
@NullMarked
@RequiredArgsConstructor
public class SecurityContext implements Facade {

    private final UserUseCase userUseCase;

    public Result<User, DomainError> getMe(String userId) {
        return userUseCase.getMe(userId);
    }

    public Result<User, DomainError> updateName(String userId, @Nullable String name) {
        return userUseCase.updateName(userId, name);
    }

    public Result<User, DomainError> updatePreferences(String userId, PreferencesPatch patch) {
        return userUseCase.updatePreferences(userId, patch);
    }
}
