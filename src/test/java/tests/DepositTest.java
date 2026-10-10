package tests;

import api.ApiErrors;
import jupiter.annotation.Account;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.TestData;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import models.rest.DepositJsonRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import service.CustomerClient;
import service.api.AccountsApiClient;
import service.api.CustomerApiClient;
import utils.Repeat;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

import static api.ApiLimits.*;
import static org.assertj.core.api.Assertions.assertThat;

@RestTest
public class DepositTest {
    private final AccountsApiClient accountsClient = new AccountsApiClient();
    private final CustomerClient customerClient = new CustomerApiClient();
    private final static long NON_EXISTENT_ACCOUNT_ID = Long.MAX_VALUE;

    // ---------- POSITIVE:  ----------
    static Stream<BigDecimal> validDepositAmounts() {
        return Stream.of(
                DEPOSIT_MIN,
                DEPOSIT_MIN.add(MINIMUM_STEP_DEPOSIT),
                DEPOSIT_MAX.subtract(MINIMUM_STEP_DEPOSIT),
                DEPOSIT_MAX
        );
    }

    @User(randomAccounts = 1)
    @ApiLogin
    @ParameterizedTest
    @MethodSource("validDepositAmounts")
    public void depositValidBoundaryAmountChangesBalanceTest(BigDecimal amount, CreateUserJsonResponse user) {
        Long accountId = Objects.requireNonNull(user.accounts().stream().findFirst().orElse(null)).id();
        accountsClient.deposit(user.username(), new DepositJsonRequest(accountId, amount));
        CustomerAccountJson accountById = customerClient.getAccountById(user.username(), accountId);
        assertThat(accountById.balance()).isEqualByComparingTo(amount);

    }

    // ---------- NEGATIVE: границы  ----------
    public static Stream<Arguments> amountInvalidData() {
        return Stream.of(
                Arguments.of(DEPOSIT_MAX.add(MINIMUM_STEP_DEPOSIT), ApiErrors.Deposit.LIMIT_5000),
                Arguments.of(DEPOSIT_MIN.subtract(MINIMUM_STEP_DEPOSIT), ApiErrors.Deposit.INVALID_AMOUNT),
                Arguments.of(MINIMUM_STEP_DEPOSIT.negate(), ApiErrors.Deposit.INVALID_AMOUNT)
        );
    }

    @User(randomAccounts = 1)
    @ApiLogin
    @ParameterizedTest
    @MethodSource("amountInvalidData")
    public void depositInvalidBoundaryAmountDoesNotChangeBalanceTest(BigDecimal amount, String errorMessage, CreateUserJsonResponse user) {
        Long accountId = Objects.requireNonNull(user.accounts().stream().findFirst().orElse(null)).id();
        accountsClient.performDepositExpectingError(user.username(),
                new DepositJsonRequest(accountId, amount),
                HttpStatus.SC_BAD_REQUEST,
                ApiErrors.KEY_MESSAGE,
                errorMessage
        );
        assertThat(customerClient.getAccountById(user.username(), accountId).balance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------- POSITIVE: накопление ----------
    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void depositSequentiallyAccumulatesBalanceTest(CreateUserJsonResponse user) {
        Long accountId = Objects.requireNonNull(user.accounts().getFirst().id());
        int count = 5;
        Repeat.repeat(count, () -> {
            accountsClient.deposit(user.username(), new DepositJsonRequest(accountId, DEPOSIT_MAX));
        });
        assertThat(customerClient.getAccountById(user.username(), accountId).balance())
                .isEqualByComparingTo(DEPOSIT_MAX.multiply(BigDecimal.valueOf(count)));
    }

    // ---------- NEGATIVE: невалидные типы / аккаунт ----------
    @User(randomAccounts = 1)
    @ApiLogin
    @Test
    public void depositToNonExistentAccountTest(CreateUserJsonResponse user) {
        accountsClient.performDepositExpectingError(user.username(),
                new DepositJsonRequest(
                        NON_EXISTENT_ACCOUNT_ID,
                        DEPOSIT_MAX),
                HttpStatus.SC_FORBIDDEN,
                ApiErrors.KEY_MESSAGE,
                ApiErrors.Auth.UNAUTHORIZED_ACCOUNT
        );

        assertThat(customerClient.getAccountById(user.username(),
                user.accounts().getFirst().id()).balance()
        ).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Data(users = {
            @User(username = "first", accounts = @Account(balance = DEPOSIT_MAX_STR)),
            @User(username = "second", randomAccounts = 1)
    })
    @ApiLogin
    @Test
    public void depositToForeignAccountDoesNotAffectBalancesTest(TestData data) {
        CreateUserJsonResponse first = data.requireByUsername("first");
        CreateUserJsonResponse second = data.requireByUsername("second");
        accountsClient.performDepositExpectingError(first.username(),
                new DepositJsonRequest(
                        second.accounts().getFirst().id(),
                        DEPOSIT_MAX),
                HttpStatus.SC_FORBIDDEN,
                ApiErrors.KEY_MESSAGE,
                ApiErrors.Auth.UNAUTHORIZED_ACCOUNT
        );

        Assertions.assertAll(() -> assertThat(customerClient.getAccountById(first.username(),
                        first.accounts().getFirst().id()).balance()
                ).isEqualByComparingTo(DEPOSIT_MAX),

                () -> assertThat(customerClient.getAccountById(second.username(),
                        second.accounts().getFirst().id()).balance()
                ).isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}