package br.cdb.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CostCenterResourceTest extends BaseHttpTest {

    private static final String CATALOG = """
            [ {"id":"d0000000-0000-0000-0000-000000000001","description":"Fixo"},
              {"id":"d0000000-0000-0000-0000-000000000002","description":"Variável"} ]""";

    @Test
    void retornaListaFixaGlobalSomenteLeitura() {
        storage.write("cost-centers.json", "costCenters", CATALOG.getBytes(StandardCharsets.UTF_8));

        asTestUser()
                .when().get("/api/cost-center")
                .then().statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is("d0000000-0000-0000-0000-000000000001"))
                .body("[0].description", is("Fixo"))
                .body("[1].description", is("Variável"));
    }

    @Test
    void naoPermiteCriacaoViaApi() {
        asTestUser()
                .body("{\"description\":\"Trabalho\"}")
                .when().post("/api/cost-center")
                .then().statusCode(405);
    }

    @Test
    void naoPermiteExclusaoViaApi() {
        asTestUser()
                .when().delete("/api/cost-center")
                .then().statusCode(405);
    }
}
