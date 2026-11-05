package tests.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerProfileJsonResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.step.UserStep;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class UpdateUserNameTest {

    @User
    @ApiLogin
    @Test
    void updateUserNameTest(CreateUserJsonResponse user) {
        String newUsername = RandomData.getName();
        Selenide.open("/edit-profile");


        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .shouldBe(Condition.visible)
                .shouldBe(Condition.clickable)
                .setValue(newUsername);


        $x("//button[contains(text(), 'Save Changes')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();

        assertThat(alertText).contains("✅ Name updated successfully!");

        CustomerProfileJsonResponse profile = UserStep.getProfile(user.username());
        Assertions.assertThat(profile.name()).isEqualTo(newUsername);
    }

    @User
    @ApiLogin
    @Test
    void updateNameWithInvalidValueTest(CreateUserJsonResponse user) {
        String newUsername = RandomData.getUsername();
        Selenide.open("/edit-profile");


        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .shouldBe(Condition.visible)
                .shouldBe(Condition.clickable)
                .setValue(newUsername);


        $x("//button[contains(text(), 'Save Changes')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();

        assertThat(alertText).contains("❌ Please enter a valid name.");

        CustomerProfileJsonResponse profile = UserStep.getProfile(user.username());
        Assertions.assertThat(profile.name()).isNull();
    }
}
