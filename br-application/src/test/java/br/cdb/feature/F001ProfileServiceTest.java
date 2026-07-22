package br.cdb.feature;

import br.cdb.context.people._0_domain.model.Person;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.context.people._1_application.service.PersonService;
import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._0_domain.Profile;
import br.cdb.feature.f001._1_application.PreferencesPatch;
import br.cdb.feature.f001._1_application.ProfileService;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class F001ProfileServiceTest {

    /** Repositório em memória — prior art: InMemoryRepositories do contexto monetary. */
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

    private InMemoryPeople people;
    private InMemoryPreferences prefs;
    private ProfileService useCase;

    @BeforeEach
    void setUp() {
        people = new InMemoryPeople();
        prefs = new InMemoryPreferences();
        Registry.remove(PersonService.class);
        Registry.set(PersonRepository.class, people);
        useCase = new ProfileService(prefs);
    }

    @Test
    void getProfileSemPreferenciasPersistidasRetornaPadrao() {
        String id = seed("Admin", Preferences.defaults());

        Result<Profile, BusinessError> result = useCase.getProfile(id);

        assertTrue(result.isSuccess());
        Profile profile = value(result);
        assertEquals("Admin", profile.person().name());
        assertEquals(Preferences.defaults(), profile.preferences());
        assertNull(profile.preferences().theme());
        assertEquals("pt-BR", profile.preferences().language());
        assertEquals("pt-BR", profile.preferences().locale());
        assertFalse(profile.preferences().sidebarCollapsed());
    }

    @Test
    void getProfilePessoaInexistenteRetornaNotFound() {
        Result<Profile, BusinessError> result = useCase.getProfile(UUID.randomUUID().toString());

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }

    @Test
    void updateNameAplicaTrim() {
        String id = seed("Admin", Preferences.defaults());

        Profile profile = value(useCase.updateName(id, "  Bruno  "));

        assertEquals("Bruno", profile.person().name());
    }

    @Test
    void updateNameEmBrancoRetornaValidationError() {
        String id = seed("Antigo", Preferences.defaults());

        Result<Profile, BusinessError> result = useCase.updateName(id, "   ");

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.Validation.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }

    @Test
    void updatePreferencesMergeParcialMantemCamposNulos() {
        String id = seed("Bruno", new Preferences("dark", "pt-BR", "pt-BR", false));

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch(null, null, null, true)));

        assertEquals("dark", profile.preferences().theme(), "theme nulo no patch mantém o atual");
        assertEquals("pt-BR", profile.preferences().language());
        assertTrue(profile.preferences().sidebarCollapsed(), "campo presente é alterado");
        assertEquals("Bruno", profile.person().name(), "preferências não afetam o nome");
    }

    @Test
    void updatePreferencesAlteraTheme() {
        String id = seed("Admin", Preferences.defaults());

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch("light", null, null, null)));

        assertEquals("light", profile.preferences().theme());
    }

    private String seed(String name, Preferences preferences) {
        UUID id = UUID.randomUUID();
        people.save(new Person(id, name, "pt-BR", "pt-BR"));
        prefs.save(id.toString(), preferences);
        return id.toString();
    }

    private static Profile value(Result<Profile, BusinessError> result) {
        assertTrue(result.isSuccess());
        Profile profile = ((Result.Success<Profile, BusinessError>) result).value();
        assertNotNull(profile);
        return profile;
    }
}
