package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class AdminPanel extends BasePage<AdminPanel> {
    private SelenideElement adminPanelText =  $(Selectors.byText("Admin Panel"));
    private SelenideElement addUserButton = $(Selectors.byText("Add User"));

    private static final String USER_ROW_TEMPLATE = "%s\nUSER";


    @Override
    public String url() {
        return "/admin";
    }

    public AdminPanel createUser(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        addUserButton.click();
        return this;
    }

    public ElementsCollection getAllUsers() {
        return $(Selectors.byText("All Users")).parent().findAll("li");
    }

    public AdminPanel shouldContainUser(String username) {
        userRow(username).shouldBe(visible);
        return this;
    }

    public AdminPanel shouldNotContainUser(String username) {
        userRow(username).shouldNot(Condition.exist);
        return this;
    }

    private SelenideElement userRow(String username) {
        return getAllUsers()
                .findBy(Condition.exactText(USER_ROW_TEMPLATE.formatted(username)));
    }
}