package tests;

import api.core.RequestExecutor;
import api.core.RestClient;
import config.Config;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@Slf4j
public class LoginUserTest implements RequestExecutor {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @ValueSource(strings = {
            "I I",
            "John Smith",
            "Самый Главный",
            "JohnJohnJohn SmithSmithSmith"
    })
    @ParameterizedTest
    public void userUpdateNameTest(String name) {
        String userName = RandomStringUtils.randomAlphabetic(10);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#",
                          "role": "USER"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        String authToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue())
                .extract()
                .header("Authorization");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .body("""
                                                {
                          "name": "names"
                        }
                        """.replace("names", name))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(name));
    }

    public static Stream<Arguments> userNameInvalidData() {
        final String error = "Name must contain two words with letters only";
        return Stream.of(
                Arguments.of("John", error),
                Arguments.of("John John John ", error),
                Arguments.of("John  John", error),
                Arguments.of(" John John", error),
                Arguments.of("John-John", error),
                Arguments.of("John 1", error),
                Arguments.of("        ", error),
                Arguments.of("", error),
                Arguments.of("John  ", error),
                Arguments.of("John John1", error),
                Arguments.of("John John%", error),
                Arguments.of("[\"John\",\"Smith\"]", error),
                Arguments.of("123", error)
        );
    }

    @MethodSource("userNameInvalidData")
    @ParameterizedTest
    public void userUpdateInvalidNameTest(String name, String errorMessage) {
        String userName = RandomStringUtils.randomAlphabetic(10);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#",
                          "role": "USER"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        String authToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue())
                .extract()
                .header("Authorization");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .body("""
                                                {
                          "name": "names"
                        }
                        """.replace("names", name))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", Matchers.equalTo(errorMessage));


        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.nullValue());
    }

    @Test
    public void userUpdateWithFakeTokenTest() {
        // Генерим Basic-токен от несуществующего пользователя
        String fakeToken = "Basic " + Base64.getEncoder()
                .encodeToString("fakeUser:fakePassword".getBytes(StandardCharsets.UTF_8));

        String userName = RandomStringUtils.randomAlphabetic(10);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#",
                          "role": "USER"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", fakeToken)
                .body("""
                                                {
                          "name": "John Smith"
                        }
                        """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        String authToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue())
                .extract()
                .header("Authorization");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.nullValue());
    }


    @Test
    public void userUpdateWithoutTokenTest() {

        String userName = RandomStringUtils.randomAlphabetic(10);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#",
                          "role": "USER"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                                {
                          "name": "John Smith"
                        }
                        """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        String authToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "userName",
                          "password": "Kate2000#"
                        }
                        """.replace("userName", userName))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue())
                .extract()
                .header("Authorization");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.nullValue());
    }

    @Test
    public void userUpdateWithTokenTest() {
        RestClient restClient = new RestClient.EmptyRestClient(Config.getInstance().frontUrl());
        executeGet(restClient.request()
                        .header("Authorization","Basic dmFkaW06VmFkaW0xMjMh"),
                "api/v1/customer/profile", UserDto.class,
                HttpStatus.SC_OK
                );

    }
}
