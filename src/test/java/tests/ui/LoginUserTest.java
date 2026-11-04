package tests.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonRequest;
import org.junit.jupiter.api.Test;
import requests.skelethon.step.AdminStep;

import static com.codeborne.selenide.Selenide.$;

@WebTest
public class LoginUserTest {


    @Test
    public void adminCanLoginWithCorrectDataTest() {
        CreateUserJsonRequest admin = CreateUserJsonRequest.builder().username("admin").password("admin").build();

        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(admin.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(admin.password());
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);
    }

    @AdminApiLogin
    @Test
    public void userCanLoginWithCorrectDataTest() {
        CreateUserJsonRequest user = AdminStep.createUser();

        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user.password());
        $("button").click();

        $(Selectors.byClassName("welcome-text")).shouldBe(Condition.visible).shouldHave(Condition.text("Welcome, noname!"));
    }
}