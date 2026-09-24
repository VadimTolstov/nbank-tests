package tests;

import models.CreateUserResponse;
import models.CustomerAccount;
import models.CreateUserRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest createUserRequest = freshUser();
        CreateUserResponse user = createUser(createUserRequest);
        CustomerAccount account = createAccount(createUserRequest);

        softly.assertThat(account)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getAccountById(user, account.getId()));
        // запросить все аккаунты пользователя и проверить, что наш аккаунт там
    }
}
