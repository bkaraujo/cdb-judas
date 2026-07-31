package br.cdb.feature.f001._1_application.usecase;

import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._0_domain.Profile;
import br.cdb.feature.f001._1_application.PreferencesPatch;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a mutação do próprio perfil da fatia {@code f001} — o par de {@code ReadUseCaseTest}
 * (era {@code F001ProfileServiceTest}, contra o serviço). O caminho HTTP completo, incluindo a
 * composição com o {@code username} de login, é de {@code F001SelfResourceTest}.
 */
class WriteUseCaseTest extends AbstractProfileTest {

    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new WriteUseCase();
    }

    @Test
    @DisplayName("updateName aplica trim")
    void updateNameAplicaTrim() {
        String id = seed("Admin", Preferences.defaults());

        Profile profile = value(useCase.updateName(id, "  Bruno  "));

        assertEquals("Bruno", profile.person().name());
    }

    @Test
    @DisplayName("updateName em branco → Validation")
    void updateNameEmBrancoRetornaValidationError() {
        String id = seed("Antigo", Preferences.defaults());

        Result<Profile, BusinessError> result = useCase.updateName(id, "   ");

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.Validation.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }

    @Test
    @DisplayName("updateName de pessoa inexistente → NotFound")
    void updateNamePessoaInexistenteRetornaNotFound() {
        Result<Profile, BusinessError> result = useCase.updateName(UUID.randomUUID().toString(), "Bruno");

        assertTrue(result.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Profile, BusinessError>) result).error());
    }

    @Test
    @DisplayName("updatePreferences faz merge parcial: campo nulo mantém o atual")
    void updatePreferencesMergeParcialMantemCamposNulos() {
        String id = seed("Bruno", new Preferences("dark", "pt-BR", "pt-BR", false));

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch(null, null, null, true)));

        assertEquals("dark", profile.preferences().theme(), "theme nulo no patch mantém o atual");
        assertEquals("pt-BR", profile.preferences().language());
        assertTrue(profile.preferences().sidebarCollapsed(), "campo presente é alterado");
        assertEquals("Bruno", profile.person().name(), "preferências não afetam o nome");
    }

    @Test
    @DisplayName("updatePreferences altera o theme")
    void updatePreferencesAlteraTheme() {
        String id = seed("Admin", Preferences.defaults());

        Profile profile = value(useCase.updatePreferences(id, new PreferencesPatch("light", null, null, null)));

        assertEquals("light", profile.preferences().theme());
    }

    @Test
    @DisplayName("updatePreferences persiste (write-through): a leitura seguinte vê o novo valor")
    void updatePreferencesPersiste() {
        String id = seed("Admin", Preferences.defaults());

        useCase.updatePreferences(id, new PreferencesPatch("light", null, null, null));

        assertEquals("light", value(new ReadUseCase().profile(id)).preferences().theme());
    }
}
