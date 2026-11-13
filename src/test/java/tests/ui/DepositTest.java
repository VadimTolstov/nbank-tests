package tests.ui;

import api.requests.skelethon.step.UserStep;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.User;
import jupiter.annotation.meta.WebTest;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Test;
import ui.pages.DepositPage;

import java.math.BigDecimal;
import java.util.Objects;

import static api.ApiLimits.*;
import static org.assertj.core.api.Assertions.assertThat;
import static ui.pages.BankAlert.USER_CREATED_DEPOSIT_FAIL;
import static ui.pages.BankAlert.USER_CREATED_DEPOSIT_SUCCESSFULLY;

@WebTest
public class DepositTest {

    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void userDepositMoneyTest(CreateUserJsonResponse user) {
        Long accountId = user.getAccountId();
        String accountNumber = user.getFirstAccountNumber();

        new DepositPage().open()
                .selectAccountNumber(accountNumber)
                .inputAmount(DEPOSIT_MAX_STR)
                .clickSaveChanges()
                .checkAlertMessageAndAccept(String.format(USER_CREATED_DEPOSIT_SUCCESSFULLY.getMessage(), DEPOSIT_MAX_STR, accountNumber));

        assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), accountId)).balance())
                .isEqualByComparingTo(DEPOSIT_MAX);
    }

    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void errorDuringUserDeposit(CreateUserJsonResponse user) {
        Long accountId = user.getAccountId();
        String accountNumber = user.getFirstAccountNumber();
        new DepositPage().open()
                .selectAccountNumber(accountNumber)
                .inputAmount("-" + DEPOSIT_MIN_STR)
                .clickSaveChanges()
                .checkAlertMessageAndAccept(USER_CREATED_DEPOSIT_FAIL.getMessage());

        assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), accountId)).balance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}