package tests.ui;

import api.requests.skelethon.step.UserStep;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class CreateAccountTest {

    @User
    @ApiLogin
    @Test
    public void userCanCreateAccountTest(CreateUserJsonResponse user) {
        new UserDashboard().open().createNewAccount();

        List<CustomerAccountJson> createdAccounts = UserStep.getAllAccounts(user.username());

        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept
                (BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().accountNumber());

        assertThat(createdAccounts.getFirst().balance()).isZero();
    }
}