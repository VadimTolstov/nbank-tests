package tests;

import models.CreateUserResponse;
import models.CustomerAccount;
import models.UserRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        UserRequest userRequest = freshUser();
        CreateUserResponse user = createUser(userRequest);
        CustomerAccount account = createAccount(userRequest);

        softly.assertThat(account)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getAccountById(user, account.getId()));
        // запросить все аккаунты пользователя и проверить, что наш аккаунт там
    }
}
