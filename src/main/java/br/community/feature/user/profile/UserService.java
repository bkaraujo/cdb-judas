package br.community.feature.user.profile;

import br.commons.Result;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Serviço da fatia {@code self} (/api/me): leitura/escrita do próprio perfil a partir da
 * identidade autenticada (o id é resolvido pela camada web e repassado). A identidade vem do
 * core ({@link UserRepository}); as preferências são uma feature à parte
 * ({@link PreferencesRepository}). Ambas são reunidas em {@link Profile}.
 */
@Service
@NullMarked
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PreferencesRepository preferences;

    public Result<Profile, DomainError> getProfile(String userId) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        return Result.success(new Profile(user, preferences.findByUserId(userId)));
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco vira nulo (exibição cai para username). */
    public Result<Profile, DomainError> updateName(String userId, @Nullable String name) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        val trimmed = (name == null || name.isBlank()) ? null : name.trim();
        val saved = repository.save(new User(user.id(), user.username(), trimmed, user.password()));
        return Result.success(new Profile(saved, preferences.findByUserId(userId)));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, DomainError> updatePreferences(String userId, PreferencesPatch patch) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new DomainError.NotFound("Usuário não encontrado"));
        val merged = preferences.findByUserId(userId).merge(patch);
        return Result.success(new Profile(user, preferences.save(userId, merged)));
    }
}
