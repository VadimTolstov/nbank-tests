package tests;

import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import models.rest.UserJson;
import org.junit.jupiter.api.Test;
import service.AccountsClient;
import service.CustomerClient;
import service.api.AccountsApiClient;
import service.api.CustomerApiClient;

@RestTest
public class CreateAccountTest {
    private final AccountsClient accountsClient = new AccountsApiClient();
    private final CustomerClient customerClient = new CustomerApiClient();

    @User
    @ApiLogin
    @Test
    public void userCanCreateAccountTest(CreateUserJsonResponse user) {
        CustomerAccountJson accountResponse = accountsClient.createAccount(
                new UserJson(user.username(),
                        user.password(),
                        user.role())
        );

        CustomerAccountJson accountById = customerClient.getAccountById(user.username(), accountResponse.id());
        ModelAssertions.assertThatModels(accountResponse, accountById).match();
    }
}
