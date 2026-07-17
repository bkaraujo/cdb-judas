package br.cdb.feature;

import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.cdb.feature.user.profile.api.PreferencesPatch;
import br.cdb.feature.user.profile.preference.Preferences;
import br.cdb.feature.user.profile.preference.PreferencesRepository;
import br.cdb.feature.user.profile.Profile;
import br.cdb.feature.user.profile.UserProfileService;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileServiceTest {

    /** Repositório em memória — prior art: InMemoryRepositories do contexto monetary. */
    static final class InMemoryUsers implements UserRepository {
        private final Map<String, User> byId = new LinkedHashMap<>();

        @Override public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }
        @Override public Optional<User> findById(String id) { return Optional.ofNullable(byId.get(id)); }
        @Override public User save(User user) { byId.put(user.id(), user); return user; }
    }

    /** Preferências em memória; sem registro retorna {@link Preferences#defaults()}. */
    static final class InMemoryPreferences implements PreferencesRepository {
        private final Map<String, Preferences> byUser = new LinkedHashMap<>();

        @Override public Preferences findByUserId(String userId) {
            return byUser.getOrDefault(userId, Preferences.defaults());
        }
        @Override public Preferences save(String userId, Preferences prefs) {
            byUser.put(userId, prefs);
            return prefs;
        }
    }

    private InMemoryUsers repo;
    private InMemoryPreferences prefs;
    private UserProfileService useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUsers();
        prefs = new InMemoryPreferences();
        useCase = new UserProfileService(repo, prefs);
    }

    @Test
    void getProfileSobreRegistroLegadoRetornaNomeNuloEPreferenciasPadrao() {
        String id = UUID.randomUUID().toString();
        // Registro legado: sem name e sem preferências persistidas → padrão na leitura.
        repo.save(new User(id, "admin", null, "hash"));

        Result<Profile, BusinessError> result = useCase.getProfile(id);

        assertTrue(result.isSuccess());
        Profile profile = value(result);
        assertNull(profile.user().name(), "name ausente permanece nulo");
        assertEquals(Preferences.defaults(), profile.preferences());
        assertNull(profile.preferences().theme());
        assertEquals("pt-BR", profile.preferences().language());
        assertEquals("pt-BR", profile.preferences().locale());
        assertFalse(profile.preferences().sidebarCollapsed());
    }

    @Test
    void getProfileUsuarioInexistenteRetornaNotFound() {
        Result<Profile, BusinessError> result = useCase.getProfile("inexistente");

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }

    @Test
    void updateNameAplicaTrim() {
        String id = seed("admin", null, Preferences.defaults());

        Profile profile = value(useCase.updateName(id, "  Bruno  "));

        assertEquals("Bruno", profile.user().name());
    }

    @Test
    void updateNameEmBrancoViraNuloEExibePeloUsername() {
        String id = seed("admin", "Antigo", Preferences.defaults());

        Profile profile = value(useCase.updateName(id, "   "));

        assertNull(profile.user().name());
        assertEquals("admin", profile.user().displayName());
    }

    @Test
    void updatePreferencesMergeParcialMantemCamposNulos() {
        String id = seed("admin", "Bruno", new Preferences("dark", "pt-BR", "pt-BR", false));

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch(null, null, null, true)));

        assertEquals("dark", profile.preferences().theme(), "theme nulo no patch mantém o atual");
        assertEquals("pt-BR", profile.preferences().language());
        assertTrue(profile.preferences().sidebarCollapsed(), "campo presente é alterado");
        assertEquals("Bruno", profile.user().name(), "preferências não afetam o nome");
    }

    @Test
    void updateNamePreservaActiveETimestampsExistentes() {
        String id = UUID.randomUUID().toString();
        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0);
        repo.save(new User(id, "admin", null, "hash", false, created, created));

        Profile profile = value(useCase.updateName(id, "Novo Nome"));

        assertFalse(profile.user().active(), "active não deve resetar pra true ao só renomear");
        assertEquals(created, profile.user().createdAt(), "createdAt não deve se perder ao só renomear");
    }

    @Test
    void updatePreferencesAlteraTheme() {
        String id = seed("admin", null, Preferences.defaults());

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch("light", null, null, null)));

        assertEquals("light", profile.preferences().theme());
    }

    private String seed(String username, String name, Preferences preferences) {
        String id = UUID.randomUUID().toString();
        repo.save(new User(id, username, name, "hash"));
        prefs.save(id, preferences);
        return id;
    }

    private static Profile value(Result<Profile, BusinessError> result) {
        assertTrue(result.isSuccess());
        Profile profile = ((Result.Success<Profile, BusinessError>) result).value();
        assertNotNull(profile);
        return profile;
    }
}
