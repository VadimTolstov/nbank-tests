package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AddDepositMoneyRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetCustomerAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest extends BaseTest {
    private static final String INVALID_MESSAGE_AMOUNT = "Invalid account or amount";
    private static final String INVALID_MESSAGE_TYPE_NULL = "Invalid field types: accountId must be integer, amount must be number";
    private static final String INVALID_MESSAGE_LIMIT_5000 = "Deposit amount exceeds the 5000 limit";
    private static final String ERROR_KEY_MESSAGE = "message";
    private static final String ERROR_KEY = "error";
    private static final String ERROR_MESSAGE = "Bad Request";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final Long NOT_EXIST_ACCOUNT_ID = 999_999_999L;
    private static final String TEST_MESSAGE_BALANCE_NOT_CHANGED = "Баланс не должен меняться ";
    private static final String TEST_MESSAGE_BALANCE_CHANGED = "Баланс должен увеличиться ровно на ";

    private UserRequest oneUser;
    private BigDecimal beforeBalanceOneUser;
    private Long accountIdOneUser;

    private RequestSpecification authUser(UserRequest user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    private GetCustomerAccountsResponse accountsOf(UserRequest user) {
        return new GetCustomerAccountsRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
    }

    private BigDecimal balanceOf(UserRequest user) {
        return accountsOf(user).stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addDeposit(UserRequest user, ResponseSpecification responseSpecification, Long accountId, BigDecimal amount) {
        addDeposit(authUser(user), responseSpecification, accountId, amount);
    }

    private void addDeposit(RequestSpecification authRequestSpecification,
                            ResponseSpecification responseSpecification,
                            Long accountId,
                            BigDecimal amount) {
        new AddDepositMoneyRequester(authRequestSpecification, responseSpecification)
                .post(new DepositRequest(accountId, amount));
    }

    private String depositBody(String accountIdJson, String amountJson) {
        return """
                {
                  "accountId": %s,
                  "amount": %s
                }
                """.formatted(accountIdJson, amountJson);
    }

    private void depositRaw(UserRequest user, String rawBody, ResponseSpecification response) {
        new AddDepositMoneyRequester(authUser(user), response).postRaw(rawBody);
    }

    private void createUser(UserRequest user) {
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user)
                .extract()
                .as(CreateUserResponse.class);
    }

    private CustomerAccount createAccount(UserRequest user) {
        return new CreateAccountRequester(authUser(user), ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().as(CustomerAccount.class);
    }

    private void assertBalanceUnchanged(UserRequest user, BigDecimal before, String msg) {
        assertEquals(0, before.compareTo(balanceOf(user)), msg);
    }

    @BeforeEach
    public void setUp() {
        oneUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(oneUser);
        accountIdOneUser = createAccount(oneUser).getId();
        beforeBalanceOneUser = balanceOf(oneUser);
    }

    // ---------- POSITIVE:  ----------

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.02", "4999.99", "5000"})
    public void depositValidBoundaryAmountChangesBalanceTest(String amount) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(oneUser, ResponseSpecs.requestReturnsOK(), accountIdOneUser, deposit);
        assertBalanceUnchanged(oneUser, beforeBalanceOneUser.add(deposit), TEST_MESSAGE_BALANCE_CHANGED + amount);
    }


    // ---------- NEGATIVE: границы  ----------
    public static Stream<Arguments> amountInvalidData() {
        return Stream.of(
                Arguments.of("5000.01", INVALID_MESSAGE_LIMIT_5000),
                Arguments.of("0.00", INVALID_MESSAGE_AMOUNT),
                Arguments.of("-0.01", INVALID_MESSAGE_AMOUNT)
        );
    }

    @ParameterizedTest
    @MethodSource("amountInvalidData")
    public void depositInvalidBoundaryAmountDoesNotChangeBalanceTest(String amount, String errorValue) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(oneUser,
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_MESSAGE, errorValue),
                accountIdOneUser,
                deposit);
        assertBalanceUnchanged(oneUser, beforeBalanceOneUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном amount: " + amount);
    }

    // ---------- POSITIVE: накопление ----------

    @Test
    public void depositSequentiallyAccumulatesBalanceTest() {
        addDeposit(oneUser, ResponseSpecs.requestReturnsOK(), accountIdOneUser, MAX_AMOUNT);
        addDeposit(oneUser, ResponseSpecs.requestReturnsOK(), accountIdOneUser, MIN_AMOUNT);
        assertBalanceUnchanged(oneUser,
                beforeBalanceOneUser.add(MAX_AMOUNT).add(MIN_AMOUNT),
                TEST_MESSAGE_BALANCE_CHANGED + MAX_AMOUNT.add(MIN_AMOUNT));

    }

    // ---------- NEGATIVE: невалидные типы / аккаунт ----------

    @Test
    public void depositToNonExistentAccountTest() {
        new AddDepositMoneyRequester(authUser(oneUser), ResponseSpecs.requestReturnsForbidden())
                .post(new DepositRequest(NOT_EXIST_ACCOUNT_ID, MAX_AMOUNT));

        assertBalanceUnchanged(oneUser, beforeBalanceOneUser, TEST_MESSAGE_BALANCE_NOT_CHANGED);
    }

    @Test
    public void depositToForeignAccountDoesNotAffectBalancesTest() {
        UserRequest twoUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(twoUser);
        // получаем id депозита второго пользователя
        Long accountIdUserTwo = createAccount(twoUser).getId();
        // получаем баланс второго пользователя
        BigDecimal balanceUserTwo = balanceOf(twoUser);
        // депозит от первого пользователя на депозит второго пользователя
        addDeposit(authUser(oneUser),
                ResponseSpecs.requestReturnsForbidden(),
                accountIdUserTwo,
                MAX_AMOUNT);
        // получаем актуальный баланс второго пользователя

        assertBalanceUnchanged(oneUser, beforeBalanceOneUser, TEST_MESSAGE_BALANCE_NOT_CHANGED);
        assertBalanceUnchanged(twoUser, balanceUserTwo, TEST_MESSAGE_BALANCE_NOT_CHANGED + "у чужого аккаунта");
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
        depositRaw(oneUser, depositBody(accountIdJson, MIN_AMOUNT.toString()),
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_MESSAGE, INVALID_MESSAGE_TYPE_NULL)
        );

        assertBalanceUnchanged(oneUser, beforeBalanceOneUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном accountId:" + accountIdJson);
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
        depositRaw(oneUser, depositBody(accountIdOneUser.toString(), amountJson),
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_MESSAGE, INVALID_MESSAGE_TYPE_NULL)
        );
        assertBalanceUnchanged(oneUser, beforeBalanceOneUser, TEST_MESSAGE_BALANCE_NOT_CHANGED + "при невалидном amount: " + amountJson);
    }

    // ---------- NEGATIVE: auth / body ----------

    @Test
    public void depositWithoutTokenTest() {
        addDeposit(RequestSpecs.invalidTokenSpec(null),
                ResponseSpecs.requestReturnsUnauthorizedRequest(),
                accountIdOneUser,
                MAX_AMOUNT
        );
    }

    @Test
    public void depositWithFakeTokenTest() {
        String fake = "Basic " + Base64.getEncoder()
                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));
        addDeposit(RequestSpecs.invalidTokenSpec(fake),
                ResponseSpecs.requestReturnsUnauthorizedRequest(),
                accountIdOneUser,
                MAX_AMOUNT
        );
    }

    @Test
    public void depositWithoutBodyTest() {
        new AddDepositMoneyRequester(authUser(oneUser),
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, ERROR_MESSAGE))
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
                """.formatted(accountIdOneUser, MAX_AMOUNT);
        depositRaw(oneUser, body, ResponseSpecs.requestReturnsOK());
        assertBalanceUnchanged(oneUser, beforeBalanceOneUser.add(MAX_AMOUNT), TEST_MESSAGE_BALANCE_CHANGED + MAX_AMOUNT);
    }
}