package tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UpdateUserNameTest extends BaseApiTest {

    private static final String INVALID_NAME_ERROR = "Name must contain two words with letters only";

    private String userName;
    private String token;

    @BeforeEach
    public void setUp() {
        userName = randomUserName();
        createUser(userName, DEFAULT_PASSWORD);
        token = loginAndGetToken(userName, DEFAULT_PASSWORD);
    }

    // ---------- positives ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "I I",
            "John Smith",
            "Самый Главный",
            "JohnJohnJohn SmithSmithSmith"
    })
    public void updateNameWithValidValueTest(String name) {
        updateName(token, name)
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        getProfile(token)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(name));
    }

    // ---------- negatives ----------

    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("John"),
                Arguments.of("John John John "),
                Arguments.of("John  John"),
                Arguments.of(" John John"),
                Arguments.of("John-John"),
                Arguments.of("John 1"),
                Arguments.of("        "),
                Arguments.of(""),
                Arguments.of("John  "),
                Arguments.of("John John1"),
                Arguments.of("John John%"),
                Arguments.of("[\"John\",\"Smith\"]"),
                Arguments.of("123"),
                Arguments.of("John\tSmith")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    public void updateNameWithInvalidValueTest(String name) {
        updateName(token, name)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", Matchers.equalTo(INVALID_NAME_ERROR));

        getProfile(token)
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.nullValue());
    }

    // ---------- negatives: bad body ----------

    public static Stream<Arguments> invalidRawBodies() {
        return Stream.of(
                Arguments.of("{\"name\": null}"),
                Arguments.of("{}"),
                Arguments.of("{\"name\": 123}"),
                Arguments.of("{\"name\": [\"John\", \"Smith\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRawBodies")
    public void updateNameWithInvalidBodyTest(String rawBody) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .body(rawBody)
                .put(BASE_URL + API_V1 + "/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void updateNameWithoutBodyTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .put(BASE_URL + API_V1 + "/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    // ---------- negatives: auth ----------

    @Test
    public void updateNameWithoutTokenTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"name\": \"John Smith\"}")
                .put(BASE_URL + API_V1 + "/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void updateNameWithFakeTokenTest() {
        String fake = "Basic " + Base64.getEncoder()
                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", fake)
                .body("{\"name\": \"John Smith\"}")
                .put(BASE_URL + API_V1 + "/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }


    private Response updateName(String token, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .body(body)
                .put(BASE_URL + API_V1 + "/customer/profile");
    }

    private Response getProfile(String token) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .get(BASE_URL + API_V1 + "/customer/profile");
    }
}