package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class DepositPage extends BasePage<DepositPage> {
    private final SelenideElement selectAccount = $x("//option[contains(text(), '-- Choose an account --')]");
    private final SelenideElement inputAmount = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private final SelenideElement buttonDeposit = $x("//button[contains(text(), 'Deposit')]");

    private final String numberAccountElement = "//option[contains(text(), '%s')]";

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage selectAccountNumber(String numberAccount) {
        selectAccount.shouldBe(Condition.clickable).click();
        $x(String.format(numberAccountElement, numberAccount)).shouldBe(Condition.visible)
                .click();
        return this;
    }

    public DepositPage inputAmount(String amount) {
        inputAmount.shouldBe(Condition.visible).sendKeys(amount);
        return this;
    }

    public DepositPage clickSaveChanges() {
        buttonDeposit.shouldBe(Condition.clickable).click();
        return this;
    }
}