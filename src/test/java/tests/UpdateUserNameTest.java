package tests;

import api.ApiErrors;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerProfileJsonResponse;
import models.rest.UpdateUserNameRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import service.api.CustomerApiClient;

import java.util.stream.Stream;

@RestTest
public class UpdateUserNameTest {
    private final CustomerApiClient customerClient = new CustomerApiClient();


    // ---------- positives ----------
    @User
    @ApiLogin
    @ParameterizedTest
    @ValueSource(strings = {
            "I I",
            "John Smith",
            "Самый Главный",
            "JohnJohnJohn SmithSmithSmith"
    })
    public void updateNameWithValidValueTest(String name, CreateUserJsonResponse user) {
        customerClient.updateUserProfileName(
                user.username(),
                new UpdateUserNameRequest(name)
        );

        CustomerProfileJsonResponse profile = customerClient.getProfile(user.username());
        Assertions.assertThat(profile.name()).isEqualTo(name);
    }

    // ---------- negatives: invalid name ----------
    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("John", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John John John ", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John  John", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of(" John John", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John-John", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John 1", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("        ", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John  ", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John John1", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("John John%", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("[\"John\",\"Smith\"]", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST),
                Arguments.of("123", ApiErrors.KEY_MESSAGE, ApiErrors.Profile.INVALID_NAME),
                Arguments.of("John\tSmith", ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST)
        );
    }

    @User
    @ApiLogin
    @ParameterizedTest()
    @MethodSource("invalidNames")
    public void updateNameWithInvalidValueTest(String name, String errorKey, String errorValue, CreateUserJsonResponse user) {
        String rawBody = "{\"name\":%s}".formatted(name);
        customerClient.updateNameExpectingError(
                user.username(),
                rawBody,
                errorKey,
                errorValue
        );

        CustomerProfileJsonResponse profile = customerClient.getProfile(user.username());
        ModelAssertions.assertThatModels(profile, user).match();
    }
}
