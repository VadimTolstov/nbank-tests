package tests;

import api.ApiErrors;
import api.ApiLimits;
import jupiter.annotation.Account;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.TestData;
import models.rest.CreateUserJsonResponse;
import models.rest.TransferJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.step.TransferStep;
import requests.skelethon.step.UserStep;
import utils.Repeat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.stream.Stream;

import static api.ApiLimits.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RestTest
public class TransferTest {

    // ---------- POSITIVE ----------
    static Stream<Arguments> validTransferAmounts() {
        BigDecimal transferBeforeMinAmount = TRANSFER_MIN.add(MINIMUM_STEP_TRANSFER);
        BigDecimal transferAfterMaxAmount = TRANSFER_MAX.subtract(MINIMUM_STEP_TRANSFER);

        return Stream.of(
                Arguments.of(TRANSFER_MIN, transferAfterMaxAmount),
                Arguments.of(transferBeforeMinAmount, TRANSFER_MAX.subtract(transferBeforeMinAmount)),
                Arguments.of(transferAfterMaxAmount, TRANSFER_MIN),
                Arguments.of(TRANSFER_MAX, TRANSFER_MAX.subtract(TRANSFER_MAX))
        );
    }

    @Data(
            users = {
                    @User(accounts = {@Account(balance = ApiLimits.TRANSFER_MAX_STR)}),
                    @User(randomAccounts = 1)
            }
    )
    @ApiLogin
    @ParameterizedTest
    @MethodSource("validTransferAmounts")
    public void transferValidAmountToSomeoneElseAccountTest(BigDecimal amountSent, BigDecimal remainingAmount, TestData data) {
        CreateUserJsonResponse firstUser = data.usersJson().getFirst();
        CreateUserJsonResponse secondUser = data.usersJson().getLast();

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();

        TransferStep.transfer(
                firstUser.username(),
                new TransferJson(firstAccountId, secondAccountId, amountSent, "")
        );

        assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(remainingAmount),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(amountSent)
        );
    }

    /**
     * Перевод себе
     */
    @User(
            accounts = {@Account(balance = ApiLimits.TRANSFER_MAX_STR)},
            randomAccounts = 1
    )
    @ApiLogin
    @Test
    public void transferToYourselfTest(CreateUserJsonResponse user) {
        Long firstAccountId = user.accounts().getLast().id();
        Long secondAccountId = user.accounts().getFirst().id();

        TransferStep.transfer(
                user.username(),
                new TransferJson(firstAccountId, secondAccountId, TRANSFER_MIN, "")
        );

        assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(TRANSFER_MAX.subtract(TRANSFER_MIN)),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(user.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(TRANSFER_MIN)
        );
    }

    /**
     * Последовательный перевод двух сумм A→B.
     */
    @Data(
            users = {
                    @User(username = "userTransferA", accounts = {@Account(balance = ApiLimits.TRANSFER_MAX_STR)}),
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
        int i = 2;
        BigDecimal transferAmount = ApiLimits.TRANSFER_MAX.divide(BigDecimal.valueOf(i), RoundingMode.HALF_UP);
        Repeat.repeat(i, () -> {
            TransferStep.transfer(
                    firstUser.username(),
                    new TransferJson(firstAccountId, secondAccountId, transferAmount, "")
            );
        });

        assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(ApiLimits.TRANSFER_MAX_STR)
        );
    }


    // ---------- NEGATIVE: границы ----------
    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(TRANSFER_MAX.add(MINIMUM_STEP_TRANSFER), ApiErrors.Transfer.LIMIT_10000),
                Arguments.of(TRANSFER_MIN.subtract(MINIMUM_STEP_TRANSFER), ApiErrors.Transfer.INVALID),
                Arguments.of(TRANSFER_MIN.negate(), ApiErrors.Transfer.INVALID)
        );
    }

    @Data(
            users = {
                    @User(accounts = {@Account(balance = TRANSFER_20000_STR)}),
                    @User(randomAccounts = 1)
            }
    )
    @ApiLogin
    @ParameterizedTest
    @MethodSource("transferInvalidData")
    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(BigDecimal amountSent, String errorMessage, TestData data) {
        CreateUserJsonResponse firstUser = data.usersJson().getFirst();
        CreateUserJsonResponse secondUser = data.usersJson().getLast();

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();
        TransferStep.transferExpectingError(
                firstUser.username(),
                new TransferJson(firstAccountId, secondAccountId, amountSent, ""),
                errorMessage
        );

        assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(TRANSFER_20000),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO)
        );
    }

    // ---------- NEGATIVE: превышение баланса ----------

    /**
     * Перевод не себе при балансе меньше суммы перевода.
     */
    @Data(
            users = {
                    @User(username = "insufficientTr1", accounts = {@Account(balance = TRANSFER_MIN_STR)}),
                    @User(username = "insufficientTr2", randomAccounts = 1)
            }
    )
    @ApiLogin
    @Test
    public void transferInsufficientFundsToForeignTest(TestData data) {
        CreateUserJsonResponse firstUser = data.requireByUsername("insufficientTr1");
        CreateUserJsonResponse secondUser = data.requireByUsername("insufficientTr2");

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();

        TransferStep.transferExpectingError(
                firstUser.username(),
                new TransferJson(firstAccountId, secondAccountId, TRANSFER_MAX, ""),
                ApiErrors.Transfer.INVALID
        );

        assertAll(
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(firstUser.username(), firstAccountId)).balance())
                        .isEqualByComparingTo(TRANSFER_MIN),
                () -> assertThat(Objects.requireNonNull(UserStep.getAccountById(secondUser.username(), secondAccountId)).balance())
                        .isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}