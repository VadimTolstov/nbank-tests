package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class TransferPage extends BasePage<TransferPage> {
    private final SelenideElement selectAccountDispatch = $x("//option[contains(text(), '-- Choose an account --')]");
    private final SelenideElement inputAmount = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private final SelenideElement inputAccountObtaining = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
    private final SelenideElement checkboxConfirm = $("#confirmCheck");
    private final SelenideElement buttonSendTransfer = $x("//button[contains(text(), 'Send Transfer')]");

    private final String numberAccountElementDispatch = "//option[contains(text(), '%s')]";

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage transferBetweenAccounts(String accountDispatch, String accountObtaining, String amount) {
        selectAccountNumber(accountDispatch)
                .inputAccountObtaining(accountObtaining)
                .inputAmount(amount)
                .checkboxConfirm()
                .clickSendTransfer();
        return this;
    }

    public TransferPage selectAccountNumber(String numberAccount) {
        selectAccountDispatch.shouldBe(Condition.clickable).click();
        $x(String.format(numberAccountElementDispatch, numberAccount))
                .shouldBe(Condition.visible)
                .click();
        return this;
    }

    public TransferPage inputAccountObtaining(String numberAccount) {
        inputAccountObtaining.shouldBe(Condition.visible).sendKeys(numberAccount);
        return this;
    }

    public TransferPage inputAmount(String amount) {
        inputAmount.shouldBe(Condition.visible).sendKeys(amount);
        return this;
    }

    public TransferPage checkboxConfirm() {
        checkboxConfirm.shouldBe(Condition.clickable).click();
        return this;
    }

    public TransferPage clickSendTransfer() {
        buttonSendTransfer.shouldBe(Condition.clickable).click();
        return this;
    }
}