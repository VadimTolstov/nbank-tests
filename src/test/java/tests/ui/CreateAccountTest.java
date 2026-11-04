package tests.ui;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.WebTest;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.step.AdminStep;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class CreateAccountTest extends TestBase {


    @AdminApiLogin
    @Test
    public void userCanCreateAccountTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке

        CreateUserJsonRequest user = AdminStep.createUser();

        String userAuthHeader = new CrudRequester(
                RequestSpecs.authAsUser(AdminConstants.PASSWORD),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.OK)
                .post(CreateUserJsonRequest.builder().username(user.username()).password(user.password()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/dashboard");

        // ШАГИ ТЕСТА
        // ШАГ 4: юзер создает аккаунт

        $(Selectors.byText("➕ Create New Account")).click();


        // ШАГ 5: проверка, что аккаунт создался на UI

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ New Account Created! Account Number:");

        alert.accept();

        Pattern pattern = Pattern.compile("Account Number: (\\w+)");
        Matcher matcher = pattern.matcher(alertText);

        matcher.find();

        String createdAccNumber = matcher.group(1);

        // ШАГ 6: проверка, что аккаунт был создан на API

        CreateUserJsonResponse[] existingUserAccounts = given()
                .spec(RequestSpecs.authAsUser(user.username()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateUserJsonResponse[].class);

        assertThat(existingUserAccounts).hasSize(1);

        CreateUserJsonResponse createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.accountMetas().getFirst().balance()).isEqualTo("0");
    }
}
