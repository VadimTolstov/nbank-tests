package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));

    public static final String WELCOME_TEXT = "Welcome, noname!";

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard shouldWelcome() {
        welcomeText.shouldBe(visible).shouldHave(text(WELCOME_TEXT));
        return this;
    }

    public UserDashboard createNewAccount() {
        createNewAccount.click();
        return this;
    }
}