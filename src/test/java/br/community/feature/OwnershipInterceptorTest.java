package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@QuarkusTest
public class OwnershipInterceptorTest extends BaseHttpTest {

    @Test
    void permiteQuandoUuidDaRotaIgualAoAutenticado() {
        asTestUser()
                .when().get("/api/" + TEST_USER_ID + "/accounts")
                .then().statusCode(200);
    }

    @Test
    void retorna403QuandoUuidDivergente() {
        String outroUsuario = UUID.randomUUID().toString();
        asTestUser()
                .when().get("/api/" + outroUsuario + "/accounts")
                .then().statusCode(403);
    }

    @Test
    void retorna403QuandoUuidMalformado() {
        asTestUser()
                .when().get("/api/nao-e-uuid/accounts")
                .then().statusCode(403);
    }

    @Test
    void streamDeOutroUsuarioRetorna403() {
        String outroUsuario = UUID.randomUUID().toString();
        asTestUser()
                .accept("text/event-stream")
                .when().get("/api/" + outroUsuario + "/stream")
                .then().statusCode(403);
    }

    @Test
    void naoGuardaRotaGlobalDeCentroDeCusto() {
        asTestUser()
                .when().get("/api/cost-center")
                .then().statusCode(200);
    }
}
