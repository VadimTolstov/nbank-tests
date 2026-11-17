package tests.ui;

import api.requests.skelethon.step.AdminStep;
import com.codeborne.selenide.Condition;
import generators.RandomModelGenerator;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.WebTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;

import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class CreateUserTest {

    @AdminApiLogin
    @Test
    public void adminCanCreateUserTest() {

        CreateUserJsonRequest newUser = RandomModelGenerator.generate(CreateUserJsonRequest.class);

        new AdminPanel().open().createUser(newUser.username(), newUser.password())
                .checkAlertMessageAndAccept(BankAlert.USER_CREATED_SUCCESSFULLY.getMessage())
                .shouldContainUser(newUser.username());

        CreateUserJsonResponse createdUser = AdminStep.getUsers().stream().filter(user -> user.username().equals(newUser.username()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @AdminApiLogin
    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        CreateUserJsonRequest newUser2 = RandomModelGenerator.generate(CreateUserJsonRequest.class);
        CreateUserJsonRequest newUser = CreateUserJsonRequest.builder()
                .password(newUser2.password())
                .username("a")
                .role(newUser2.role())
                .build();

        new AdminPanel().open().createUser(newUser.username(), newUser.password())
                .checkAlertMessageAndAccept(BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .shouldNotContainUser(newUser.username());

        long usersWithSameUsernameAsNewUser = AdminStep.getUsers().stream()
                .filter(user -> user.username().equals(newUser.username())).count();

        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}