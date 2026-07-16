package br.cdb.feature;

import br.cdb.core.web.security.User;
import br.cdb.feature.system.auth.LoginResource;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;

/**
 * IDOR de conta/cartão: {@code accountId}/{@code cardId} que chegam por path, query ou corpo —
 * fora do primeiro segmento {@code /api/{uuid}/…} — não têm nenhuma verificação de propriedade
 * antes do {@link br.cdb.feature.user.UserGuards}. Cobre exatamente os caminhos que um filtro
 * JAX-RS por path jamais pegaria.
 */
@QuarkusTest
class UserGuardsIdorTest extends BaseHttpTest {

    private static final String OTHER_USER_ID = "00000000-0000-0000-0000-0000000000be";

    @BeforeEach
    void seedOtherUser() {
        userRepository.save(new User(OTHER_USER_ID, "other-user", null, BcryptUtil.bcryptHash("test")));
    }

    private RequestSpecification asOtherUser() {
        return RestAssured.given()
                .header(LoginResource.TOKEN_HEADER, tokenStore.issue(OTHER_USER_ID))
                .contentType(ContentType.JSON);
    }

    private UUID createAccount(RequestSpecification requester, String ownerUuid, String color) {
        String json = "{\"name\":\"Conta\",\"type\":\"CHECKING\",\"color\":\"%s\",\"active\":true}".formatted(color);
        String id = requester
                .body(json)
                .when().post("/api/" + ownerUuid + "/accounts")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }

    private UUID createCard(RequestSpecification requester, String ownerUuid, UUID accountId, String last4) {
        String id = requester
                .body("{\"last4\":\"%s\"}".formatted(last4))
                .when().post("/api/" + ownerUuid + "/accounts/" + accountId + "/cards")
                .then().statusCode(201)
                .extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }

    @Test
    void contaAlheiaRetorna404EmVezDeVazarDados() {
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");

        asTestUser()
                .queryParam("year", "2024")
                .when().get("/api/" + TEST_USER_ID + "/accounts/" + foreignAccountId + "/balance")
                .then().statusCode(404);

        asOtherUser()
                .queryParam("year", "2024")
                .when().get("/api/" + OTHER_USER_ID + "/accounts/" + foreignAccountId + "/balance")
                .then().statusCode(200);
    }

    @Test
    void transferComContasAlheiasNoCorpoRetorna404() {
        val foreignFrom = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");
        val foreignTo = createAccount(asOtherUser(), OTHER_USER_ID, "#222222");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-07-01","amount":50.00}
            """.formatted(foreignFrom, foreignTo);

        asTestUser()
                .body(transferJson)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/transfer")
                .then().statusCode(404);
    }

    @Test
    void confirmInvoiceImportComCartaoAlheioRetorna422CardNotFound() {
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");
        val foreignCardId = createCard(asOtherUser(), OTHER_USER_ID, foreignAccountId, "9999");

        String body = """
            {"type":"CREDIT_CARD_INVOICE","rows":[
              {"description":"Mercado","amount":80.00,"date":"2025-07-10","originalDate":"2025-07-10","categoryId":"%s","cardId":"%s"}
            ]}
            """.formatted(UUID.randomUUID(), foreignCardId);

        asTestUser()
                .body(body)
                .when().post("/api/" + TEST_USER_ID + "/accounts/transactions/import/confirm")
                .then().statusCode(422)
                .body("code", is("CARD_NOT_FOUND"));
    }

    @Test
    void patchContaAlheiaRetorna404() {
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");
        String patchJson = """
            {"name":"Hijack","type":"CHECKING","color":"#000000","active":true}
            """;

        asTestUser()
                .body(patchJson)
                .when().patch("/api/" + TEST_USER_ID + "/accounts/" + foreignAccountId)
                .then().statusCode(404);
    }

    @Test
    void deleteContaAlheiaRetorna404() {
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");

        asTestUser()
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + foreignAccountId)
                .then().statusCode(404);

        asOtherUser()
                .when().get("/api/" + OTHER_USER_ID + "/accounts/" + foreignAccountId)
                .then().statusCode(200);
    }

    @Test
    void criarCartaoEmContaAlheiaRetorna404() {
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");

        asTestUser()
                .body("{\"last4\":\"4321\"}")
                .when().post("/api/" + TEST_USER_ID + "/accounts/" + foreignAccountId + "/cards")
                .then().statusCode(404);
    }

    @Test
    void deleteComMoveParaContaAlheiaRetorna404() {
        val myAccountId = createAccount(asTestUser(), TEST_USER_ID, "#333333");
        val foreignAccountId = createAccount(asOtherUser(), OTHER_USER_ID, "#111111");

        asTestUser()
                .queryParam("strategy", "MOVE")
                .queryParam("targetId", foreignAccountId.toString())
                .when().delete("/api/" + TEST_USER_ID + "/accounts/" + myAccountId)
                .then().statusCode(404);
    }
}
