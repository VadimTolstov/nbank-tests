package tests;

import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import models.rest.CreateUserJsonRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserJsonRequest createUserJsonRequest = freshUser();
        CreateUserJsonResponse user = createUser(createUserJsonRequest);
        CustomerAccountJson account = createAccount(createUserJsonRequest);

        softly.assertThat(account)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getAccountById(user, account.getId()));
        // запросить все аккаунты пользователя и проверить, что наш аккаунт там
    }
}
