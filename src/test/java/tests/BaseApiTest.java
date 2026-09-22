package tests;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class BaseApiTest {

    protected static final String BASE_URL = "http://localhost:4111";
    protected static final String API_V1 = "/api/v1";
    protected static final String ADMIN_AUTH = "Basic YWRtaW46YWRtaW4=";   // admin:admin
    protected static final String DEFAULT_PASSWORD = "Kate2000#";

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    // ---------- helpers ----------

    protected String randomUserName() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    protected void createUser(String userName, String password) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", ADMIN_AUTH)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(userName, password))
                .post(BASE_URL + API_V1 + "/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);
    }

    protected String loginAndGetToken(String userName, String password) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(userName, password))
                .post(BASE_URL + API_V1 + "/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue())
                .extract()
                .header("Authorization");
    }

    /** Создаёт аккаунт пользователю и возвращает его id. */
    protected long createAccount(String token, String userName, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .body("""
                        {
                          "customer": {
                            "username": "%s",
                            "password": "%s",
                            "role": "USER"
                          }
                        }
                        """.formatted(userName, password))
                .post(BASE_URL + API_V1 + "/accounts");

        response.then().assertThat().statusCode(HttpStatus.SC_CREATED);
        return response.jsonPath().getLong("id");
    }

    /** POST /accounts/deposit. token == null → без Authorization. */
    protected Response deposit(String token, Object accountId, Object amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", accountId);
        body.put("amount", amount);

        RequestSpecification request = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body);
        if (token != null) {
            request = request.header("Authorization", token);
        }
        return request.post(BASE_URL + API_V1 + "/accounts/deposit");
    }

    /** POST /accounts/transfer. token == null → без Authorization. */
    protected Response transfer(String token, Object senderId, Object receiverId, Object amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("senderAccountId", senderId);
        body.put("receiverAccountId", receiverId);
        body.put("amount", amount);

        RequestSpecification request = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body);
        if (token != null) {
            request = request.header("Authorization", token);
        }
        return request.post(BASE_URL + API_V1 + "/accounts/transfer");
    }

    /**
     * Возвращает баланс аккаунта по id.
     * Предполагаем, что GET /customer/accounts возвращает список объектов
     * с полями "id" и "balance".
     */
    protected BigDecimal getBalance(String token, long accountId) {
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .get(BASE_URL + API_V1 + "/customer/accounts");
        response.then().assertThat().statusCode(HttpStatus.SC_OK);

        List<Map<String, Object>> accounts = response.jsonPath().getList("$");
        for (Map<String, Object> acc : accounts) {
            Object id = acc.get("id");
            if (id instanceof Number n && n.longValue() == accountId) {
                return new BigDecimal(acc.get("balance").toString());
            }
        }
        throw new IllegalStateException("Account " + accountId + " not found");
    }
}