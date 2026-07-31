package br.cdb.feature.f001._1_application.usecase;

import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._0_domain.Profile;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura do próprio perfil da fatia {@code f001} — o par de {@code WriteUseCaseTest}
 * (era {@code F001ProfileServiceTest}, contra o serviço). O caminho HTTP completo, incluindo a
 * composição com o {@code username} de login, é de {@code F001SelfResourceTest}.
 */
class ReadUseCaseTest extends AbstractProfileTest {

    private ReadUseCase reads;

    @BeforeEach
    void setUp() {
        reads = new ReadUseCase();
    }

    @Test
    @DisplayName("profile sem preferências persistidas devolve os padrões")
    void getProfileSemPreferenciasPersistidasRetornaPadrao() {
        String id = seed("Admin", Preferences.defaults());

        Result<Profile, BusinessError> result = reads.profile(id);

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
    @DisplayName("profile devolve as preferências persistidas da pessoa")
    void getProfileDevolvePreferenciasPersistidas() {
        String id = seed("Bruno", new Preferences("dark", "en-US", "en-US", true));

        Profile profile = value(reads.profile(id));

        assertEquals("dark", profile.preferences().theme());
        assertEquals("en-US", profile.preferences().language());
        assertTrue(profile.preferences().sidebarCollapsed());
    }

    @Test
    @DisplayName("profile de pessoa inexistente → NotFound")
    void getProfilePessoaInexistenteRetornaNotFound() {
        Result<Profile, BusinessError> result = reads.profile(UUID.randomUUID().toString());

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }
}
