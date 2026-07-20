package br.cdb.feature.user.profile;

import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.cdb.feature.user.profile.api.PreferencesPatch;
import br.cdb.feature.user.profile.preference.PreferencesRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serviço da fatia {@code self} (/api/me): leitura/escrita do próprio perfil a partir da
 * identidade autenticada (o id é resolvido pela camada web e repassado). A identidade vem do
 * core ({@link UserRepository}); as preferências são uma feature à parte
 * ({@link PreferencesRepository}). Ambas são reunidas em {@link Profile}.
 */
@Singleton
@NullMarked
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository repository;
    private final PreferencesRepository preferences;

    public Result<Profile, BusinessError> getProfile(String personId) {
        val user = repository.findByPersonId(personId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));
        return Result.success(new Profile(user, preferences.findByPersonId(personId)));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, BusinessError> updatePreferences(String personId, PreferencesPatch patch) {
        val user = repository.findByPersonId(personId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));
        val merged = preferences.findByPersonId(personId).merge(patch);
        return Result.success(new Profile(user, preferences.save(personId, merged)));
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco vira nulo (exibição cai para username). */
    public Result<Profile, BusinessError> updateName(String personId, @Nullable String name) {
        val user = repository.findByPersonId(personId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));

        val trimmed = (name == null || name.isBlank()) ? null : name.trim();
        val saved = repository.save(new User(user.id(), user.username(), trimmed, user.password(), user.active(), user.createdAt(), user.updatedAt()));

        return Result.success(new Profile(saved, preferences.findByPersonId(personId)));
    }
}
