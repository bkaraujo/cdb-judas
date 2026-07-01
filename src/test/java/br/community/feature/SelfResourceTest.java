package br.community.feature;

import br.community.core.web.security.User;
import br.community.feature.user.profile.Preferences;
import br.community.feature.user.profile.PreferencesRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integração HTTP do recurso self. {@link BaseHttpTest#asTestUser()} já exercita a cadeia real de
 * filtros (autenticação por token + autorização); aqui também cobrimos a ausência de token (401).
 */
@QuarkusTest
public class SelfResourceTest extends BaseHttpTest {

    @Inject
    PreferencesRepository preferencesRepository;

    @Test
    void getMeAutenticadoRetorna200ComPerfil() {
        userRepository.save(new User(TEST_USER_ID, "tester", "Tester", "hash"));
        preferencesRepository.save(TEST_USER_ID, Preferences.defaults());

        asTestUser()
                .when().get("/api/me")
                .then().statusCode(200)
                .body("id", is(TEST_USER_ID))
                .body("username", is("tester"))
                .body("name", is("Tester"))
                .body("preferences.theme", nullValue())
                .body("preferences.language", is("pt-BR"))
                .body("preferences.locale", is("pt-BR"))
                .body("preferences.sidebarCollapsed", is(false));
    }

    @Test
    void getMeSemTokenRetorna401() {
        RestAssured.given()
                .when().get("/api/me")
                .then().statusCode(401);
    }

    @Test
    void patchSomenteNomeNaoAfetaPreferencias() {
        userRepository.save(new User(TEST_USER_ID, "tester", "Tester", "hash"));
        preferencesRepository.save(TEST_USER_ID, new Preferences("dark", "pt-BR", "pt-BR", false));

        asTestUser()
                .body("{\"name\":\"Renomeado\"}")
                .when().patch("/api/me")
                .then().statusCode(200)
                .body("name", is("Renomeado"))
                .body("preferences.theme", is("dark"))
                .body("preferences.sidebarCollapsed", is(false));
    }

    @Test
    void patchSomentePreferenciasNaoAfetaNome() {
        userRepository.save(new User(TEST_USER_ID, "tester", "Mantido", "hash"));

        asTestUser()
                .body("{\"preferences\":{\"theme\":\"light\"}}")
                .when().patch("/api/me")
                .then().statusCode(200)
                .body("name", is("Mantido"))
                .body("preferences.theme", is("light"))
                .body("preferences.language", is("pt-BR"));
    }

    @Test
    void patchSemTokenRetorna401() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"x\"}")
                .when().patch("/api/me")
                .then().statusCode(401);
    }
}
