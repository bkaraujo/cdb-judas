package br.cdb.feature.f001._1_application.service;

import br.cdb.feature.f001._0_domain.model.Profile;
import br.cdb.feature.f001._0_domain.repository.PreferencesRepository;
import br.cdb.feature.f001._1_application.usecase.PreferencesPatch;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Serviço da fatia {@code self} (/api/me): leitura/escrita do próprio perfil a partir da
 * identidade autenticada (o personId é resolvido pela camada web e repassado). A identidade/nome vem
 * do par de use cases de {@code f000} (kernel — leitura e rename da pessoa); as preferências são
 * uma feature à parte ({@link PreferencesRepository}). Ambas são reunidas em {@link Profile}. Esta fatia não conhece
 * o agregado {@code User} (login) — o {@code username} é resolvido à parte por
 * {@code SelfResource} (via {@code UserRepository#findByPersonId}), na borda.
 *
 * <p>Context-wired como os demais serviços de fatia (o par {@code ReadUseCase}/{@code WriteUseCase}
 * o resolve com {@code Context.tryGet}); sem CDI.
 */
@NullMarked
public class ProfileService {

    // Par de f000 (kernel): a pessoa mora lá, o perfil aqui. Context-wired, nunca @Inject.
    private final br.cdb.feature.f000._1_application.usecase.ReadUseCase personReads =
            Context.tryGet(br.cdb.feature.f000._1_application.usecase.ReadUseCase.class);
    private final br.cdb.feature.f000._1_application.usecase.WriteUseCase personWrites =
            Context.tryGet(br.cdb.feature.f000._1_application.usecase.WriteUseCase.class);

    private final PreferencesRepository preferences = Context.get(PreferencesRepository.class);

    public Result<Profile, BusinessError> getProfile(String personId) {
        return personReads.person(personId).map(person -> new Profile(person, preferences.findByPersonId(personId)));
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, BusinessError> updatePreferences(String personId, PreferencesPatch patch) {
        return personReads.person(personId).map(person -> {
            val merged = preferences.findByPersonId(personId).merge(patch);
            return new Profile(person, preferences.save(personId, merged));
        });
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco é rejeitado. */
    public Result<Profile, BusinessError> updateName(String personId, String name) {
        val trimmed = name.trim();
        if (trimmed.isBlank()) return Result.failure(new BusinessError.Validation("f000.person.newNameBlank"));

        return personReads.person(personId)
                .flatMap(person -> personWrites.renamePerson(person, trimmed))
                .map(saved -> new Profile(saved, preferences.findByPersonId(personId)));
    }
}
