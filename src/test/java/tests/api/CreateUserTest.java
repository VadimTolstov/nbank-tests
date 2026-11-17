package tests.api;

import api.ApiErrors;
import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import generators.RandomModelGenerator;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.RestTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import models.rest.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.step.AdminStep;

import java.util.stream.Stream;

@RestTest
public class CreateUserTest {

    @AdminApiLogin
    @Test
    public void adminCanCreateUserWithCorrectData() {
        CreateUserJsonRequest userJsonRequest = RandomModelGenerator.generate(CreateUserJsonRequest.class);

        CreateUserJsonResponse userJsonResponse = AdminStep.createUser(userJsonRequest);

        ModelAssertions.assertThatModels(userJsonRequest, userJsonResponse).match();
        ModelAssertions.assertThatModels(userJsonRequest, AdminStep.getUserById(userJsonResponse.id())).match();
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of("   ", "Password33$", UserRole.USER, ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_BLANK),
                Arguments.of("ab", "Password33$", UserRole.USER, ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_LENGTH),
                Arguments.of("abc$", "Password33$", UserRole.USER, ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_INVALID_CHARS),
                Arguments.of("abc%", "Password33$", UserRole.USER, ApiErrors.KEY_USERNAME, ApiErrors.User.USERNAME_INVALID_CHARS)
        );
    }

    @AdminApiLogin
    @MethodSource("userInvalidData")
    @ParameterizedTest(name = "[{index}] username={0} → {4}")
    public void adminCanNotCreateUserWithInvalidData(String username, String password, UserRole role, String errorKey, String errorValue) {
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.errorWithField(errorKey, errorValue)
        ).post(new CreateUserJsonRequest(username, password, role));

        Assertions.assertNull(AdminStep.getUserByUsername(username));
    }
}