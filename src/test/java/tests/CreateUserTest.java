package tests;

import generators.RandomData;
import models.UserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {
    @Test
    public void adminCanCreateUserWithCorrectData() {
        UserRequest userRequest = freshUser();
        CreateUserResponse createUserResponse =  createUser(userRequest);
        createUserResponse = getUserById(createUserResponse);
        softly.assertThat(userRequest.getUsername()).isEqualTo(createUserResponse.getUsername());
        softly.assertThat(userRequest.getPassword()).isNotEqualTo(createUserResponse.getPassword());
        softly.assertThat(userRequest.getRole()).isEqualTo(createUserResponse.getRole());

    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of("   ", "Password33$", "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", "Password33$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc$", "Password33$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("abc%", "Password33$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots")
        );

    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, UserRole role, String errorKey, String errorValue) {
        UserRequest userRequest = UserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue))
                .post(userRequest);
            Assertions.assertNull(getUserByName(username));
    }
}