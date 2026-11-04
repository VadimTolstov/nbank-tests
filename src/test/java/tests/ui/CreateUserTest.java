package tests.ui;

import api.spec.RequestSpecs;
import com.codeborne.selenide.*;
import generators.RandomModelGenerator;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.WebTest;
import models.comparison.ModelAssertions;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;

import java.util.Arrays;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebTest
public class CreateUserTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.100.2:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @AdminApiLogin
    @Test
    public void adminCanCreateUserTest() {
        // ШАГ 1: админ залогинился в банке
        CreateUserJsonRequest admin = CreateUserJsonRequest.builder().username("admin").password("admin").build();

        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(admin.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(admin.password());
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        // ШАГ 2: админ создает юзера в банке
        CreateUserJsonRequest newUser = RandomModelGenerator.generate(CreateUserJsonRequest.class);

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.password());
        $(Selectors.byText("Add User")).click();


        // ШАГ 3: проверка, что алерт "✅ User created successfully!"
        Alert alert = switchTo().alert();

        assertEquals(alert.getText(), "✅ User created successfully!");

        alert.accept();

        // ШАГ 4: проверка, что юзер отображается на UI

        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.username() + "\nUSER")).shouldBe(Condition.visible);

        // ШАГ 5: проверка, что юзер создан на API

        CreateUserJsonResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserJsonResponse[].class);

        CreateUserJsonResponse createdUser = Arrays.stream(users).filter(user -> user.username().equals(newUser.username()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @AdminApiLogin
    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        // ШАГ 1: админ залогинился в банке
        CreateUserJsonRequest admin = CreateUserJsonRequest.builder().username("admin").password("admin").build();

        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(admin.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(admin.password());
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        // ШАГ 2: админ создает юзера в банке
        CreateUserJsonRequest newUser2 = RandomModelGenerator.generate(CreateUserJsonRequest.class);
        CreateUserJsonRequest newUser = CreateUserJsonRequest.builder()
                .password(newUser2.password())
                .username("a")
                .role(newUser2.role())
                .build();

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.username());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.password());
        $(Selectors.byText("Add User")).click();


        // ШАГ 3: проверка, что алерт "Username must be between 3 and 15 characters"
        Alert alert = switchTo().alert();

        assertThat(alert.getText()).contains("Username must be between 3 and 15 characters");

        alert.accept();

        // ШАГ 4: проверка, что юзер НЕ отображается на UI

        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.username() + "\nUSER")).shouldNotBe(Condition.exist);

        // ШАГ 5: проверка, что юзер НЕ создан на API

        CreateUserJsonResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserJsonResponse[].class);

        long usersWithSameUsernameAsNewUser = Arrays.stream(users).filter(user -> user.username().equals(newUser.username())).count();

        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}