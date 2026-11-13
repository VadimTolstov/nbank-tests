package tests.ui;

import api.requests.skelethon.step.UserStep;
import generators.RandomData;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerProfileJsonResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.pages.UserProfile;

import static ui.pages.BankAlert.USER_UPDATED_NAME_FAIL;
import static ui.pages.BankAlert.USER_UPDATED_NAME_SUCCESS;

@WebTest
public class UpdateUserNameTest {

    @User
    @ApiLogin
    @Test
    void updateUserNameTest(CreateUserJsonResponse user) {
        String newUsername = RandomData.getName();

        new UserProfile().open()
                .setUserName(newUsername)
                .clickSaveChanges()
                .checkAlertMessageAndAccept(USER_UPDATED_NAME_SUCCESS.getMessage());

        CustomerProfileJsonResponse profile = UserStep.getProfile(user.username());
        Assertions.assertThat(profile.name()).isEqualTo(newUsername);
    }

    @User
    @ApiLogin
    @Test
    void updateNameWithInvalidValueTest(CreateUserJsonResponse user) {
        new UserProfile().open()
                .setUserName(RandomData.getUsername())
                .clickSaveChanges()
                .checkAlertMessageAndAccept(USER_UPDATED_NAME_FAIL.getMessage());

        CustomerProfileJsonResponse profile = UserStep.getProfile(user.username());
        Assertions.assertThat(profile.name()).isNull();
    }
}
