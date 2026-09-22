package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
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
        updateProfileName(name, ResponseSpecs.nameIsInvalid());

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
    }

    // ---------- negatives: bad body ----------
    public static Stream<Arguments> invalidRawBodies() {
        return Stream.of(
                Arguments.of("{\"name\": 12 3}"),
                Arguments.of("{\"name\": [\"John\", \"Smith\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRawBodies")
    public void updateNameWithInvalidBodyTest(String rawBody) {
        updateProfileRaw(rawBody, ResponseSpecs.requestIsMalformed());

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
    }

    @Test
    public void updateNameWithoutBodyTest() {
        updateProfileNoBody(ResponseSpecs.requestIsMalformed());

        GetUserProfileResponse profile = fetchProfile();

        softly.assertThat(profile.getUsername()).isEqualTo(createUser.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(createUser.getRole());
        softly.assertThat(profile.getName()).isNull();
    }

    // ---------- negatives: auth ----------
    public static Stream<Arguments> invalidAuthSpecs() {
        return Stream.of(
                Arguments.of(RequestSpecs.invalidTokenSpec(null), "без токена"),
                Arguments.of(RequestSpecs.invalidTokenSpec(RandomData.getFakeToken()), "поддельный токен")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAuthSpecs")
    public void updateNameWithInvalidAuthDoesNotTokenTest(RequestSpecification invalidSpec, String caseName) {
        new UpdateUserNameRequester(invalidSpec,
                ResponseSpecs.requestReturnsUnauthorizedRequest())
                .put(new UpdateUserNameRequest(RandomData.getUsername()));
    }
}