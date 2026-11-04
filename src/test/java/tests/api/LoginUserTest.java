package tests.api;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.RestTest;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.AuthRequester;
import requests.skelethon.step.AdminStep;

@RestTest
public class LoginUserTest {

    @Test
    public void adminCanGenerateAuthTokenTest() {
        String token = new AuthRequester(
                RequestSpecs.defaultSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.OK
        ).login(AdminConstants.LOGIN, AdminConstants.PASSWORD);

        Assertions.assertNotNull(token);
    }

    @AdminApiLogin
    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserJsonRequest user = AdminStep.createUser();

        String token = new AuthRequester(
                RequestSpecs.defaultSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.OK
        ).login(user.username(), user.password());

        Assertions.assertNotNull(token);
    }
}