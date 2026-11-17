package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class UserProfile extends BasePage<UserProfile> {
    private final SelenideElement userNameField = $(Selectors.byAttribute("placeholder", "Enter new name"));
    private final SelenideElement buttenSaveChanges = $x("//button[contains(text(), 'Save Changes')]");

    @Override
    public String url() {
        return "/edit-profile";
    }

    public UserProfile setUserName(String userName) {
        userNameField.shouldBe(Condition.visible)
                .shouldBe(Condition.clickable)
                .setValue(userName);
        return this;
    }

    public UserProfile clickSaveChanges() {
        buttenSaveChanges.shouldBe(Condition.clickable)
                .click();
        return this;
    }
}