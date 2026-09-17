package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UserRequest;
import models.CreateUserResponse;
import models.GetUserProfileResponse;
import models.UpdateUserNameRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.GetProfileRequester;
import requests.UpdateUserNameRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateUserNameTest extends BaseTest {

    private static final String INVALID_NAME_ERROR = "Name must contain two words with letters only";
    private static final String ERROR_KEY_NAME = "message";
    private static final String ERROR_KEY = "error";
    private static final String INVALID_ERROR = "Bad Request";

    private UserRequest createUser;


    private RequestSpecification authUser() {
        return RequestSpecs.authAsUser(createUser.getUsername(), createUser.getPassword());
    }

    private GetUserProfileResponse fetchProfile() {
        return new GetProfileRequester(authUser(), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetUserProfileResponse.class);
    }

    private void updateProfileName(String name, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(), response)
                .put(new UpdateUserNameRequest(name));
    }

    private void updateProfileRaw(String rawBody, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(), response)
                .putRaw(rawBody);
    }

    private void updateProfileNoBody(ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(), response)
                .putNoBody();
    }


    @BeforeEach
    public void setUp() {
        createUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUser)
                .extract()
                .as(CreateUserResponse.class);
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
        updateProfileName(name, ResponseSpecs.requestReturnsOK());

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isEqualTo(name);
    }

    // ---------- negatives: invalid name ----------

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
        updateProfileName(name, ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_NAME, INVALID_NAME_ERROR));

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
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
        updateProfileRaw(rawBody, ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, INVALID_ERROR));

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
    }

    @Test
    public void updateNameWithoutBodyTest() {
        updateProfileNoBody(ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, INVALID_ERROR));

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
    }

    // ---------- negatives: auth ----------

    @Test
    public void updateNameWithoutTokenTest() {
        new UpdateUserNameRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorizedRequest())
                .put(new UpdateUserNameRequest(RandomData.getUsername()));
    }

    @Test
    public void updateNameWithFakeTokenTest() {
        String fake = RandomData.getFakeToken();

        new UpdateUserNameRequester(RequestSpecs.invalidTokenSpec(fake),
                ResponseSpecs.requestReturnsUnauthorizedRequest())
                .put(new UpdateUserNameRequest(RandomData.getUsername()));
    }
}