package br.cdb.feature.f001._1_application;

import br.cdb.feature.f000._1_application.usecase.PersonUseCase;
import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._0_domain.Profile;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Serviço da fatia {@code self} (/api/me): leitura/escrita do próprio perfil a partir da
 * identidade autenticada (o personId é resolvido pela camada web e repassado). A identidade/nome vem
 * do contexto people, via {@link PersonUseCase}; as preferências são uma feature à parte
 * ({@link PreferencesRepository}). Ambas são reunidas em {@link Profile}. Esta fatia não conhece
 * o agregado {@code User} (login) — o {@code username} é resolvido à parte por
 * {@code SelfResource} (via {@code UserRepository#findByPersonId}), na borda.
 */
@Singleton
@NullMarked
@RequiredArgsConstructor
public class ProfileService {

    private final PersonUseCase personUseCase = new PersonUseCase();
    private final PreferencesRepository preferences = Context.get(PreferencesRepository.class);

    public Result<Profile, BusinessError> getProfile(String personId) {
        return personUseCase.findById(personId).map(person -> new Profile(person, preferences.findByPersonId(personId)));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, BusinessError> updatePreferences(String personId, PreferencesPatch patch) {
        return personUseCase.findById(personId).map(person -> {
            val merged = preferences.findByPersonId(personId).merge(patch);
            return new Profile(person, preferences.save(personId, merged));
        });
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco é rejeitado. */
    public Result<Profile, BusinessError> updateName(String personId, String name) {
        val trimmed = name.trim();
        if (trimmed.isBlank()) return Result.failure(new BusinessError.Validation("Novo nome não pode estar em branco"));

        return personUseCase.findById(personId)
                .flatMap(person -> personUseCase.rename(person, trimmed))
                .map(saved -> new Profile(saved, preferences.findByPersonId(personId)));
    }
}
