package tests.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import api.requests.skelethon.step.UserStep;

import java.math.BigDecimal;
import java.util.Objects;

import static api.ApiLimits.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class DepositTest {

    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void userDepositMoneyTest(CreateUserJsonResponse user) {
        Long accountId = Objects.requireNonNull(user.accounts().getFirst().id());
        String accountNumber = Objects.requireNonNull(user.accounts().getFirst().accountNumber());
        Selenide.open("/deposit");

        $x("//option[contains(text(), '-- Choose an account --')]")
                .shouldBe(Condition.clickable)
                .click();
        $x("//option[contains(text(), '" + accountNumber + "')]")
                .shouldBe(Condition.visible)
                .click();

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .shouldBe(Condition.visible)
                .shouldBe(Condition.clickable)
                .setValue(DEPOSIT_MAX_STR);

        $x("//button[contains(text(), 'Deposit')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();
        Assertions.assertAll(
                () -> assertThat(alertText).contains("✅ Successfully deposited $" + DEPOSIT_MAX_STR + " to account " + accountNumber),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), accountId)).balance())
                        .isEqualByComparingTo(DEPOSIT_MAX)
        );
    }

    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void errorDuringUserDeposit(CreateUserJsonResponse user) {
        Long accountId = Objects.requireNonNull(user.accounts().getFirst().id());
        String accountNumber = Objects.requireNonNull(user.accounts().getFirst().accountNumber());
        Selenide.open("/deposit");

        $x("//option[contains(text(), '-- Choose an account --')]")
                .shouldBe(Condition.clickable)
                .click();
        $x("//option[contains(text(), '" + accountNumber + "')]")
                .shouldBe(Condition.visible)
                .click();

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .shouldBe(Condition.visible)
                .shouldBe(Condition.clickable)
                .setValue("-" + DEPOSIT_MIN_STR);

        $x("//button[contains(text(), 'Deposit')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();
        Assertions.assertAll(
                () -> assertThat(alertText).contains("❌ Please enter a valid amount."),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), accountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}