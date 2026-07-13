package br.cdb.feature.user.profile;

import br.cdb.core.web.security.UserRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

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

    public Result<Profile, BusinessError> getProfile(String userId) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));
        return Result.success(new Profile(user, preferences.findByUserId(userId)));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, BusinessError> updatePreferences(String userId, PreferencesPatch patch) {
        val user = repository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));
        val merged = preferences.findByUserId(userId).merge(patch);
        return Result.success(new Profile(user, preferences.save(userId, merged)));
    }
}
