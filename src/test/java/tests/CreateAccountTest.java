package tests;

import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import models.rest.UserJson;
import org.junit.jupiter.api.Test;
import service.AccountsClient;
import service.api.AccountsApiClient;

import java.math.BigDecimal;

@RestTest
public class CreateAccountTest extends BaseTest {
    private final AccountsClient accountsClient = new AccountsApiClient();

    @User
    @ApiLogin
    @Test
    public void userCanCreateAccountTest(CreateUserJsonResponse user) {
        CustomerAccountJson account = accountsClient.createAccount(
                new UserJson(user.getUsername(),
                        user.getPassword(),
                        user.getRole())
        );
//todo надо добавить систему сравнения модели или что то аналогичное
        softly.assertThat(account)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getAccountById(user, account.getId()));
    }
}
