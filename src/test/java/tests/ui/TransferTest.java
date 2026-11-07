package tests.ui;

import api.ApiLimits;
import api.requests.skelethon.step.UserStep;
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
import ui.pages.TransferPage;

import java.math.BigDecimal;
import java.util.Objects;

import static api.ApiLimits.TRANSFER_MAX_STR;
import static api.ApiLimits.TRANSFER_MIN_STR;
import static org.assertj.core.api.Assertions.assertThat;
import static ui.pages.BankAlert.TRANSFER_FAILED_INSUFFICIENT_FUNDS;
import static ui.pages.BankAlert.USER_TRANSFER_SUCCESSFULLY;

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

        Long firstAccountId = firstUser.getAccountId();
        Long secondAccountId = secondUser.getAccountId();

        String firstAccountNumber = firstUser.getFirstAccountNumber();
        String secondAccountNumber = secondUser.getFirstAccountNumber();

        new TransferPage().open()
                .transferBetweenAccounts(
                        firstAccountNumber,
                        secondAccountNumber,
                        TRANSFER_MAX_STR
                ).checkAlertMessageAndAccept(String.format(
                        USER_TRANSFER_SUCCESSFULLY.getMessage(),
                        TRANSFER_MAX_STR,
                        secondAccountNumber
                ));
        Assertions.assertAll(
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

        Long firstAccountId = firstUser.getAccountId();
        Long secondAccountId = secondUser.getAccountId();

        String firstAccountNumber = firstUser.getFirstAccountNumber();
        String secondAccountNumber = secondUser.getFirstAccountNumber();

        new TransferPage().open()
                .transferBetweenAccounts(
                        firstAccountNumber,
                        secondAccountNumber,
                        TRANSFER_MAX_STR
                ).checkAlertMessageAndAccept(TRANSFER_FAILED_INSUFFICIENT_FUNDS.getMessage());

        Assertions.assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(ApiLimits.TRANSFER_MIN_STR),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}