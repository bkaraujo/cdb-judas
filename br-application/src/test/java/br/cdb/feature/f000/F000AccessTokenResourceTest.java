package br.cdb.feature.f000;

import br.cdb.BaseHttpTest;
import br.cdb.core.web.HTTPRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contrato do token de acesso pela borda — {@code AccessTokenStore} não tem teste de unidade
 * (política dos três tipos em {@code docs/backend/testing.md}), então o que ele garante é
 * verificado pelo que a API responde.
 *
 * <p>Não coberto aqui: a expiração por ociosidade (30min). Exercitá-la pela borda exigiria esperar
 * o TTL ou torná-lo configurável — e um TTL curto no perfil de teste derrubaria a sessão no meio de
 * todos os outros testes HTTP.
 */
@QuarkusTest
public class F000AccessTokenResourceTest extends BaseHttpTest {

    private static String login() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"admin\"}")
                .when().post("/login")
                .then().statusCode(200)
                .extract().header(HTTPRequest.TOKEN_HEADER);
    }

    /** Sessão única: logar de novo revoga a sessão anterior do mesmo usuário. */
    @Test
    void novoLoginRevogaSessaoAnteriorDoMesmoUsuario() {
        val primeiro = login();
        val segundo = login();
        assertNotEquals(primeiro, segundo, "cada login emite um token novo");

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, primeiro)
                .when().get("/api/version")
                .then().statusCode(401);

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, segundo)
                .when().get("/api/version")
                .then().statusCode(200);
    }

    /** Token é de uso único: rotacionado na resposta, o anterior morre. */
    @Test
    void tokenUsadoNaoValeDuasVezes() {
        val token = login();

        val rotacionado = RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, token)
                .when().get("/api/version")
                .then().statusCode(200)
                .extract().header(HTTPRequest.TOKEN_HEADER);

        assertNotNull(rotacionado, "resposta bem-sucedida emite o próximo token");
        assertNotEquals(token, rotacionado);

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, token)
                .when().get("/api/version")
                .then().statusCode(401);
    }

    /**
     * Isolamento entre usuários: a revogação de {@link #novoLoginRevogaSessaoAnteriorDoMesmoUsuario}
     * é escopada por usuário — a sessão de um não derruba a de outro. É o que o mapa por
     * {@code userId} garante e o que precisa continuar valendo com mais de um usuário.
     */
    @Test
    void sessaoDeUmUsuarioNaoDerrubaADeOutro() {
        val doTestUser = tokenStore.open(TEST_USER_ID, TEST_USER_ID, TEST_USERNAME).token();
        val doAdmin = login();

        // Novo login do admin: revoga só o dele.
        val doAdminRenovado = login();

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, doAdmin)
                .when().get("/api/version")
                .then().statusCode(401);

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, doAdminRenovado)
                .when().get("/api/version")
                .then().statusCode(200);

        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, doTestUser)
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200);
    }

    @Test
    void tokenDesconhecidoRetorna401() {
        RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, UUID.randomUUID().toString().replace("-", ""))
                .when().get("/api/version")
                .then().statusCode(401);
    }
}
