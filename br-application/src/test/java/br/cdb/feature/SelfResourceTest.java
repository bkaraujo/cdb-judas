package br.cdb.feature;

import br.cdb.context.people._0_domain.model.Person;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.core.security.User;
import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._0_domain.PreferencesRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integração HTTP do recurso self. {@link BaseHttpTest#asTestUser()} já exercita a cadeia real de
 * filtros (autenticação por token + autorização); aqui também cobrimos a ausência de token (401).
 * {@code username} é semeado via {@link User} (login); {@code name} via {@link Person} (contexto
 * people) — as duas fatias não se tocam, então cada fixture semeia seu próprio agregado.
 */
@QuarkusTest
public class SelfResourceTest extends BaseHttpTest {

    @Inject
    PreferencesRepository preferencesRepository;

    @Inject
    PersonRepository personRepository;

    @Test
    void getMeAutenticadoRetorna200ComPerfil() {
        userRepository.save(new User(TEST_USER_ID, "tester", null, "hash"));
        seedName("Tester");
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
        userRepository.save(new User(TEST_USER_ID, "tester", null, "hash"));
        seedName("Tester");
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
        userRepository.save(new User(TEST_USER_ID, "tester", null, "hash"));
        seedName("Mantido");

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

    private void seedName(String name) {
        personRepository.save(new Person(UUID.fromString(TEST_USER_ID), name, "pt-BR", "pt-BR"));
    }
}
