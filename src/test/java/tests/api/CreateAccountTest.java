package tests.api;

import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.skelethon.step.UserStep;
import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@RestTest
public class CreateAccountTest {

    @User
    @ApiLogin
    @Test
    public void userCanCreateAccountTest(CreateUserJsonResponse user) {
        CustomerAccountJson accountResponse = (CustomerAccountJson) new ValidatedCrudRequester<>(
                RequestSpecs.authAsUser(user.username()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.CREATED
        ).post(new CreateUserJsonRequest(user.username(), user.password(), user.role()));


        CustomerAccountJson accountById = UserStep.getAccountById(user.username(), accountResponse.id());
        Assertions.assertNotNull(accountById);
        ModelAssertions.assertThatModels(accountResponse, accountById).match();
    }
}
