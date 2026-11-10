package tests.ui;

import api.ApiLimits;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import jupiter.annotation.Account;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.TestData;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import requests.skelethon.step.UserStep;

import java.math.BigDecimal;
import java.util.Objects;

import static api.ApiLimits.TRANSFER_MAX_STR;
import static api.ApiLimits.TRANSFER_MIN_STR;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static org.assertj.core.api.Assertions.assertThat;

@WebTest
public class TransferTest {

    @Data(
            users = {
                    @User(username = "userTransferA", accounts = {@Account(balance = TRANSFER_MAX_STR)}),
                    @User(username = "userTransferB", randomAccounts = 1)
            }
    )
    @ApiLogin
    @Test
    public void transferSequentiallySamePairTest(TestData data) {
        CreateUserJsonResponse firstUser = data.requireByUsername("userTransferA");
        CreateUserJsonResponse secondUser = data.requireByUsername("userTransferB");

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();

        String firstAccountNumber = firstUser.accounts().getFirst().accountNumber();
        String secondAccountNumber = secondUser.accounts().getFirst().accountNumber();

        Selenide.open("/transfer");

        $x("//option[contains(text(), '-- Choose an account --')]")
                .shouldBe(Condition.clickable)
                .click();

        $x("//option[contains(text(), '" + firstAccountNumber + "')]")
                .shouldBe(Condition.visible)
                .click();


        $(Selectors.byAttribute("placeholder", "Enter recipient account number"))
                .shouldBe(Condition.visible)
                .setValue(secondAccountNumber);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .shouldBe(Condition.visible)
                .setValue(TRANSFER_MAX_STR);

        $("#confirmCheck").shouldBe(Condition.clickable).click();

        $x("//button[contains(text(), 'Send Transfer')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();
        Assertions.assertAll(
                () -> assertThat(alertText).contains("✅ Successfully transferred $" + TRANSFER_MAX_STR + " to account " + secondAccountNumber + "!"),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(ApiLimits.TRANSFER_MAX_STR)
        );
    }


    @Data(
            users = {
                    @User(username = "userTransfer1", accounts = {@Account(balance = TRANSFER_MIN_STR)}),
                    @User(username = "userTransfer2", randomAccounts = 1)
            }
    )
    @ApiLogin
    @Test
    public void errorTransferSequentiallySamePairTest(TestData data) {
        CreateUserJsonResponse firstUser = data.requireByUsername("userTransfer1");
        CreateUserJsonResponse secondUser = data.requireByUsername("userTransfer2");

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();

        String firstAccountNumber = firstUser.accounts().getFirst().accountNumber();
        String secondAccountNumber = secondUser.accounts().getFirst().accountNumber();

        Selenide.open("/transfer");

        $x("//option[contains(text(), '-- Choose an account --')]")
                .shouldBe(Condition.clickable)
                .click();

        $x("//option[contains(text(), '" + firstAccountNumber + "')]")
                .shouldBe(Condition.visible)
                .click();

        $(Selectors.byAttribute("placeholder", "Enter recipient account number"))
                .shouldBe(Condition.visible)
                .setValue(secondAccountNumber);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .shouldBe(Condition.visible)
                .setValue(TRANSFER_MAX_STR);

        $("#confirmCheck").shouldBe(Condition.clickable).click();

        $x("//button[contains(text(), 'Send Transfer')]")
                .shouldBe(Condition.clickable)
                .click();

        String alertText = Selenide.confirm();
        Assertions.assertAll(
                () -> assertThat(alertText).contains("❌ Error: Invalid transfer: insufficient funds or invalid accounts"),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(ApiLimits.TRANSFER_MIN_STR),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}