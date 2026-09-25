package tests;

import generators.RandomData;
import models.rest.AdminCredentials;
import models.rest.LoginUserRequest;
import models.rest.CreateUserJsonRequest;
import models.rest.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import requests.LoginUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class LoginUserTest extends BaseTest {

    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest userRequest = LoginUserRequest.builder()
                .username(AdminCredentials.LOGIN.getValue())
                .password(AdminCredentials.PASSWORD.getValue())
                .build();

        new LoginUserRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(userRequest);
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserJsonRequest createUserJsonRequest = CreateUserJsonRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(createUserJsonRequest);

        new LoginUserRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(createUserJsonRequest.getUsername()).password(createUserJsonRequest.getPassword()).build())
                .header("Authorization", Matchers.notNullValue());
    }
}