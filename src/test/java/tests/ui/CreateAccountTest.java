package tests.ui;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import io.restassured.common.mapper.TypeRef;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonRequest;
import models.rest.CustomerAccountJson;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.AuthRequester;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.GetAccountsRequester;
import requests.skelethon.step.AdminStep;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class CreateAccountTest {

    @AdminApiLogin
    @Test
    public void userCanCreateAccountTest() {
        // ШАГ 1-2: админ логинится и создает юзера
        CreateUserJsonRequest user = AdminStep.createUser();

        // ШАГ 3: юзер логинится в банке (через API)
        String userAuthHeader = new AuthRequester(
                RequestSpecs.defaultSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.OK
        ).login(user.username(), user.password());

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/dashboard");

        // ШАГ 4: юзер создает аккаунт на UI
        $(Selectors.byText("➕ Create New Account")).click();

        // ШАГ 5: проверка UI
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ New Account Created! Account Number:");
        alert.accept();

        Pattern pattern = Pattern.compile("Account Number: (\\w+)");
        Matcher matcher = pattern.matcher(alertText);
        matcher.find();
        String createdAccNumber = matcher.group(1);

        // ШАГ 6: проверка через API
        List<CustomerAccountJson> existingUserAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(user.username()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.OK
        ).getEndpoint();

        assertThat(existingUserAccounts).hasSize(1);

        CustomerAccountJson createdAccount = existingUserAccounts.get(0);

        assertThat(createdAccount.accountNumber()).isEqualTo(createdAccNumber);
        assertThat(createdAccount.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}