package tests;

import api.ApiErrors;
import generators.RandomModelGenerator;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.RestTest;
import models.rest.CreateUserJsonResponse;
import models.rest.UserJson;
import models.rest.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import service.AdminClient;
import service.api.AdminApiClient;

import java.util.stream.Stream;

@RestTest
public class CreateUserTest extends BaseTest {
    private final AdminClient adminClient = new AdminApiClient();

    @AdminApiLogin
    @Test
    public void adminCanCreateUserWithCorrectData() {
        UserJson userJson = RandomModelGenerator.generate(UserJson.class);
        CreateUserJsonResponse userResponse = adminClient.createUsers(userJson);
        userResponse = adminClient.getUserById(userResponse.getId());
        //todo добавить проверку модели ответа
        softly.assertThat(userResponse.getUsername()).isEqualTo(userJson.username());
        softly.assertThat(userResponse.getPassword()).isNotEqualTo(userJson.password());
        softly.assertThat(userResponse.getRole()).isEqualTo(userJson.role());
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of("   ", "Password33$", UserRole.USER,  ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_BLANK),
                Arguments.of("ab", "Password33$", UserRole.USER,  ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_LENGTH),
                Arguments.of("abc$", "Password33$", UserRole.USER,  ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_INVALID_CHARS),
                Arguments.of("abc%", "Password33$", UserRole.USER,  ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_INVALID_CHARS)
        );
    }

    @AdminApiLogin
    @MethodSource("userInvalidData")
    @ParameterizedTest(name = "[{index}] username={0} → {4}")
    public void adminCanNotCreateUserWithInvalidData(String username, String password, UserRole role, String errorKey, String errorValue) {
        ((AdminApiClient) adminClient).createUsersExpectingError(new UserJson(username, password, role), errorKey, errorValue);

        Assertions.assertNull(getUserByName(username));
    }
}