package br.community.feature;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class DashboardResourceTest extends BaseHttpTest {

    @Test
    void deveObterResultadoMensal() {
        asTestUser()
                .queryParam("month", "3")
                .queryParam("year", "2024")
                .when().get("/api/" + TEST_USER_ID + "/dashboard/result")
                .then().statusCode(200)
                .body("incomes", notNullValue())
                .body("expenses", notNullValue())
                .body("result", notNullValue())
                .body("history", instanceOf(java.util.List.class));
    }
}
