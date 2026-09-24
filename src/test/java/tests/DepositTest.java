package tests;

import generators.RandomData;
import io.restassured.specification.ResponseSpecification;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AddDepositMoneyRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static api.ApiLimits.*;

public class DepositTest extends BaseTest {
    private static final String TEST_MESSAGE_BALANCE_NOT_CHANGED = "Баланс не должен меняться ";
    private static final String TEST_MESSAGE_BALANCE_CHANGED = "Баланс должен увеличиться ровно на ";

    private CreateUserRequest firstUser;
    private BigDecimal beforeBalanceFirstUser;
    private Long accountIdFirstUser;

    private void assertBalanceEquals(CreateUserRequest user, BigDecimal before, String msg) {
        assertEquals(0, before.compareTo(balanceOf(user)), msg);
    }

    @BeforeEach
    public void setUp() {
        firstUser =freshUser();

        createUser(firstUser);
        accountIdFirstUser = createAccount(firstUser).getId();
        beforeBalanceFirstUser = balanceOf(firstUser);
    }

    // ---------- POSITIVE:  ----------
    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.02", "4999.99", "5000"})
    public void depositValidBoundaryAmountChangesBalanceTest(String amount) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(firstUser, ResponseSpecs.requestReturnsOK(), accountIdFirstUser, deposit);
        assertBalanceEquals(firstUser, beforeBalanceFirstUser.add(deposit), TEST_MESSAGE_BALANCE_CHANGED + amount);
    }

    // ---------- NEGATIVE: границы  ----------
    public static Stream<Arguments> amountInvalidData() {
        return Stream.of(
                Arguments.of("5000.01", ResponseSpecs.depositLimitExceeded()),
                Arguments.of("0.00", ResponseSpecs.amountOrAccountIsInvalid()),
                Arguments.of("-0.01", ResponseSpecs.amountOrAccountIsInvalid())
        );
    }

    @ParameterizedTest
    @MethodSource("amountInvalidData")
    public void depositInvalidBoundaryAmountDoesNotChangeBalanceTest(String amount,ResponseSpecification responseSpecs) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(firstUser,
                responseSpecs,
                accountIdFirstUser,
                deposit);
        assertBalanceEquals(firstUser, beforeBalanceFirstUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном amount: " + amount);
    }

    // ---------- POSITIVE: накопление ----------
    @Test
    public void depositSequentiallyAccumulatesBalanceTest() {
        addDeposit(firstUser, ResponseSpecs.requestReturnsOK(), accountIdFirstUser, DEPOSIT_MAX);
        addDeposit(firstUser, ResponseSpecs.requestReturnsOK(), accountIdFirstUser, DEPOSIT_MIN);
        assertBalanceEquals(firstUser,
                beforeBalanceFirstUser.add(DEPOSIT_MAX).add(DEPOSIT_MIN),
                TEST_MESSAGE_BALANCE_CHANGED + DEPOSIT_MAX.add(DEPOSIT_MIN));

    }

    // ---------- NEGATIVE: невалидные типы / аккаунт ----------
    @Test
    public void depositToNonExistentAccountTest() {
        new AddDepositMoneyRequester(authUser(firstUser), ResponseSpecs.requestReturnsForbidden())
                .post(new DepositRequest(NOT_EXIST_ACCOUNT_ID, DEPOSIT_MAX));

        assertBalanceEquals(firstUser, beforeBalanceFirstUser, TEST_MESSAGE_BALANCE_NOT_CHANGED);
    }

    @Test
    public void depositToForeignAccountDoesNotAffectBalancesTest() {
        CreateUserRequest secondUser = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(secondUser);
        // получаем id депозита второго пользователя
        Long accountIdSecondUser = createAccount(secondUser).getId();
        // получаем баланс второго пользователя
        BigDecimal balanceSecondUser = balanceOf(secondUser);
        // депозит от первого пользователя на депозит второго пользователя
        addDeposit(authUser(firstUser),
                ResponseSpecs.requestReturnsForbidden(),
                accountIdSecondUser,
                DEPOSIT_MAX);
        // получаем актуальный баланс второго пользователя

        assertBalanceEquals(firstUser, beforeBalanceFirstUser, TEST_MESSAGE_BALANCE_NOT_CHANGED);
        assertBalanceEquals(secondUser, balanceSecondUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "у чужого аккаунта");
    }

    // ---------- NEGATIVE: невалидный тип accountId ----------
    public static Stream<Arguments> invalidAccountIdBodies() {
        return Stream.of(
                Arguments.of("\"abc\""),
                Arguments.of("null"),
                Arguments.of("[1, 2]"),
                Arguments.of("{\"x\": 1}")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAccountIdBodies")
    public void depositWithInvalidAccountIdTypeDoesNotChangeBalanceTest(String accountIdJson) {
        depositRaw(firstUser, depositBody(accountIdJson, DEPOSIT_MIN.toString()),
                ResponseSpecs.fieldTypesAreInvalid()
        );

        assertBalanceEquals(firstUser, beforeBalanceFirstUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном accountId:" + accountIdJson);
    }

// ---------- NEGATIVE: невалидный тип amount ----------
    public static Stream<Arguments> invalidAmountBodies() {
        return Stream.of(
                Arguments.of("\"0.01\""),
                Arguments.of("\"abc\""),
                Arguments.of("[0.01]"),
                Arguments.of("null"),
                Arguments.of("{\"x\": 0.01}")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAmountBodies")
    public void depositWithInvalidAmountTypeDoesNotChangeBalanceTest(String amountJson) {
        depositRaw(firstUser, depositBody(accountIdFirstUser.toString(), amountJson),
                ResponseSpecs.fieldTypesAreInvalid()
        );
        assertBalanceEquals(firstUser, beforeBalanceFirstUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном amount: " + amountJson);
    }

    // ---------- NEGATIVE: auth / body ----------
    @Test
    public void depositWithoutTokenTest() {
        addDeposit(RequestSpecs.invalidTokenSpec(null),
                ResponseSpecs.requestReturnsUnauthorizedRequest(),
                accountIdFirstUser,
                DEPOSIT_MAX
        );
    }

    @Test
    public void depositWithFakeTokenTest() {

        addDeposit(RequestSpecs.invalidTokenSpec(RandomData.getFakeToken()),
                ResponseSpecs.requestReturnsUnauthorizedRequest(),
                accountIdFirstUser,
                DEPOSIT_MAX
        );
    }

    @Test
    public void depositWithoutBodyTest() {
        new AddDepositMoneyRequester(authUser(firstUser),
                ResponseSpecs.requestIsMalformed())
                .postNoBody();
    }

    @Test
    public void depositWithExtraFieldsTest() {
        // лишние поля должны игнорироваться, запрос валиден и баланс растёт
        String body = """
                {
                    "accountId": %d,
                        "amount": %s,
                        "hack": "yes"
                }
                """.formatted(accountIdFirstUser, DEPOSIT_MAX);
        depositRaw(firstUser, body, ResponseSpecs.requestReturnsOK());
        assertBalanceEquals(firstUser, beforeBalanceFirstUser.add(DEPOSIT_MAX), TEST_MESSAGE_BALANCE_CHANGED + DEPOSIT_MAX);
    }
}