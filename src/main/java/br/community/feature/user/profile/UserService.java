package br.community.feature.user.profile;

import br.commons.Result;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.core.web.security.PreferencesPatch;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Serviço da fatia {@code self} (/api/me): leitura/escrita do próprio usuário a partir da
 * identidade autenticada (o id é resolvido pela camada web e repassado). Substitui o antigo
 * {@code UserUseCase} do contexto de segurança — agora uma fatia vertical em Spring.
 */
@Service
@NullMarked
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public Result<User, DomainError> getMe(String userId) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        return Result.success(user);
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco vira nulo (exibição cai para username). */
    public Result<User, DomainError> updateName(String userId, @Nullable String name) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        val trimmed = (name == null || name.isBlank()) ? null : name.trim();
        return Result.success(repository.save(new User(user.id(), user.username(), trimmed, user.password(), user.preferences())));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<User, DomainError> updatePreferences(String userId, PreferencesPatch patch) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        val merged = user.preferences().merge(patch);
        return Result.success(repository.save(new User(user.id(), user.username(), user.name(), user.password(), merged)));
    }
}
