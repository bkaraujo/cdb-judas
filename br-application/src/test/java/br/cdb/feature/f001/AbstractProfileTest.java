package br.cdb.feature.f001;

import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._0_domain.Profile;
import br.cdb.feature.f001._1_application.service.ProfileService;
import br.cdb.feature.f001._1_application.usecase.ReadUseCase;
import br.cdb.feature.f001._1_application.usecase.WriteUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture comum do par {@code ReadUseCase}/{@code WriteUseCase} de {@code f001} — repositórios em
 * memória publicados no {@code Context} (prior art: {@code InMemoryRepositories} do pacote
 * {@code br.cdb.context.monetary}, que não cobre pessoa/preferências).
 */
abstract class AbstractProfileTest {

    /** Pessoas em memória. */
    static final class InMemoryPeople implements PersonRepository {
        private final Map<UUID, Person> byId = new LinkedHashMap<>();

        @Override public List<Person> findAll() { return List.copyOf(byId.values()); }
        @Override public Optional<Person> findById(UUID id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Person save(Person person) { byId.put(person.id(), person); return person; }
        @Override public void deleteById(UUID id) { byId.remove(id); }
        @Override public void clearCache() { /* no-op */ }
    }

    /** Preferências em memória; sem registro retorna {@link Preferences#defaults()}. */
    static final class InMemoryPreferences implements PreferencesRepository {
        private final Map<String, Preferences> byUser = new LinkedHashMap<>();

        @Override public Preferences findByPersonId(String userId) {
            return byUser.getOrDefault(userId, Preferences.defaults());
        }
        @Override public Preferences save(String userId, Preferences prefs) {
            byUser.put(userId, prefs);
            return prefs;
        }
    }

    protected InMemoryPeople people;
    protected InMemoryPreferences prefs;

    @BeforeEach
    void setUpProfileGraph() {
        people = new InMemoryPeople();
        prefs = new InMemoryPreferences();
        // PersonService é registrado por F000Module em produção; aqui, sobre o repositório fake.
        Context.set(PersonRepository.class, () -> people);
        Context.set(PersonService.class, () -> new PersonService(people));
        Context.set(PreferencesRepository.class, () -> prefs);
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes do teste anterior.
        // O par de f000 entra na lista porque ProfileService o resolve (a pessoa mora no kernel);
        // FQN porque o nome simples colide com o par desta fatia.
        Context.remove(ProfileService.class);
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        Context.remove(br.cdb.feature.f000._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f000._1_application.usecase.WriteUseCase.class);
    }

    protected String seed(String name, Preferences preferences) {
        UUID id = UUID.randomUUID();
        people.save(new Person(id, name, "pt-BR", "pt-BR"));
        prefs.save(id.toString(), preferences);
        return id.toString();
    }

    protected static Profile value(Result<Profile, BusinessError> result) {
        assertTrue(result.isSuccess());
        Profile profile = ((Result.Success<Profile, BusinessError>) result).value();
        assertNotNull(profile);
        return profile;
    }
}
