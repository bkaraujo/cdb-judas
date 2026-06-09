package br.community.context.security._1_application.usecase;

import br.commons.Result;
import br.community.context.security._0_domain.UserRepository;
import br.community.context.security._0_domain.model.User;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Caso de uso do recurso {@code self}. Orquestra a leitura/escrita do próprio usuário
 * a partir da identidade autenticada (o id é resolvido pela camada web e repassado).
 * Prior art: {@code AccountUseCase}.
 */
@NullMarked
@RequiredArgsConstructor
public class UserUseCase {

    private final UserRepository repository;

    public Result<User, DomainError> getMe(String userId) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        return Result.success(user);
    }
}
