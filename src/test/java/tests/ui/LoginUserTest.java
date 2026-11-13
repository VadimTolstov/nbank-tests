package tests.ui;

import com.codeborne.selenide.Condition;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.LoginPage;
import ui.pages.UserDashboard;

@WebTest
public class LoginUserTest {


    @Test
    public void adminCanLoginWithCorrectDataTest() {
        new LoginPage().open().login(AdminConstants.LOGIN, AdminConstants.PASSWORD)
                .getPage(AdminPanel.class).getAdminPanelText().shouldBe(Condition.visible);
    }

    @User
    @Test
    public void userCanLoginWithCorrectDataTest(CreateUserJsonResponse user) {
        new LoginPage().open().login(user.username(), user.password())
                .getPage(UserDashboard.class)
                .shouldWelcome();
    }
}