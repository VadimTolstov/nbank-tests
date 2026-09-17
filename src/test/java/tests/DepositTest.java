package tests;

import generators.RandomData;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest extends BaseTest {
    private static final String INVALID_MESSAGE = "Invalid account or amount";
    private static final String INVALID_MESSAGE_TYPE_NULL = "Invalid field types: accountId must be integer, amount must be number";
    private static final String INVALID_MESSAGE_LIMIT_5000 = "Deposit amount exceeds the 5000 limit";
    private static final String ERROR_KEY = "message";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final Long NOT_EXIST_ACCOUNT_ID = 999_999_999L;
    private UserRequest createUser;
    private BigDecimal beforeBalance;
    private Long accountId;

    private RequestSpecification authUser() {
        return RequestSpecs.authAsUser(createUser.getUsername(), createUser.getPassword());
    }

    private GetCustomerAccountsResponse getAccountResponse() {
        return new GetCustomerAccountsRequester(authUser(), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
    }

    private @NotNull BigDecimal getAccountBalance() {
        return getAccountResponse().stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addDeposit(ResponseSpecification responseSpecification, BigDecimal amount) {
        new AddDepositMoneyRequester(authUser(), responseSpecification)
                .post(new DepositRequest(accountId, amount));
    }

    private String userName;
    private String token;

    @BeforeEach
    public void setUp() {
        createUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUser)
                .extract()
                .as(CreateUserResponse.class);

        new CreateAccountRequester(authUser(), ResponseSpecs.entityWasCreated())
                .post(null);

        beforeBalance = getAccountBalance();
        accountId = getAccountResponse().stream()
                .mapToLong(CustomerAccount::getId).findFirst().getAsLong();
    }

    // ---------- POSITIVE:  ----------

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.02", "4999.99", "5000"})
    public void depositValidBoundaryAmountChangesBalanceTest(String amount) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(ResponseSpecs.requestReturnsOK(), deposit);
        assertEquals(0, beforeBalance.add(deposit).compareTo(getAccountBalance()),
                "Баланс должен увеличиться ровно на " + amount);
    }


    // ---------- NEGATIVE: границы  ----------
    public static Stream<Arguments> amountInvalidData() {
        return Stream.of(
                Arguments.of("5000.01", INVALID_MESSAGE_LIMIT_5000),
                Arguments.of("0.00", INVALID_MESSAGE),
                Arguments.of("-0.01", INVALID_MESSAGE)
        );
    }

    @ParameterizedTest
    @MethodSource("amountInvalidData")
    public void depositInvalidBoundaryAmountDoesNotChangeBalanceTest(String amount, String errorValue) {
        BigDecimal deposit = new BigDecimal(amount);
        addDeposit(ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, errorValue), deposit);
        assertEquals(0, beforeBalance.compareTo(getAccountBalance()),
                "Баланс не должен меняться при невалидной сумме " + amount);
    }

    // ---------- POSITIVE: накопление ----------

    @Test
    public void depositSequentiallyAccumulatesBalanceTest() {
        addDeposit(ResponseSpecs.requestReturnsOK(), MAX_AMOUNT);
        addDeposit(ResponseSpecs.requestReturnsOK(), MIN_AMOUNT);
        assertEquals(0, beforeBalance.add(MAX_AMOUNT).add(MIN_AMOUNT)
                .compareTo(getAccountBalance()));
    }

    // ---------- NEGATIVE: невалидные типы / аккаунт ----------

    @Test
    public void depositWithNullAmountDoesNotChangeBalanceTest() {
        addDeposit(ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, INVALID_MESSAGE_TYPE_NULL), null);
        assertEquals(0, beforeBalance.compareTo(getAccountBalance()));
    }

    @Test
    public void depositToNonExistentAccountTest() {
        new AddDepositMoneyRequester(authUser(), ResponseSpecs.requestReturnsForbidden())
                .post(new DepositRequest(NOT_EXIST_ACCOUNT_ID, MAX_AMOUNT));

        assertEquals(0, beforeBalance.compareTo(getAccountBalance()),
                "Баланс нашего аккаунта не должен измениться");
    }

    @Test
    public void depositToForeignAccountDoesNotAffectBalancesTest() {
        UserRequest twoUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();
        // создаём второго пользователя
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(twoUser)
                .extract()
                .as(CreateUserResponse.class);
        // получаем его токен
        RequestSpecification tokenUserTwo = RequestSpecs.authAsUser(twoUser.getUsername(), twoUser.getPassword());
        // создаём аккаунт для второго пользователя
        new CreateAccountRequester(tokenUserTwo, ResponseSpecs.entityWasCreated())
                .post(null);
        // получаем данные об аккаунтах второго пользователя
        GetCustomerAccountsResponse accountsUserTwo = new GetCustomerAccountsRequester(tokenUserTwo, ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
        // получаем баланс второго пользователя
        BigDecimal balanceUserTwo = accountsUserTwo.stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // получаем id депозита второго пользователя
        Long accountIdUserTwo = accountsUserTwo.stream()
                .mapToLong(CustomerAccount::getId).findFirst().getAsLong();
        // депозит от первого пользователя на депозит второго пользователя
        new AddDepositMoneyRequester(authUser(), ResponseSpecs.requestReturnsForbidden())
                .post(new DepositRequest(accountIdUserTwo, MAX_AMOUNT));
        // получаем актуальный баланс второго пользователя
        BigDecimal afterBalanceUserTwo = new GetCustomerAccountsRequester(tokenUserTwo, ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class)
                .stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, beforeBalance.compareTo(getAccountBalance()),
                "Наш баланс не должен измениться");
        assertEquals(0, balanceUserTwo.compareTo(afterBalanceUserTwo),
                "Баланс чужого аккаунта не должен измениться");
    }

    //todo: добавить тест с невалидным типом данных
    @Test
    public void depositWithStringAccountIdDoesNotChangeBalanceTest() {
        new AddDepositMoneyRequester(authUser(), ResponseSpecs.requestReturnsBadRequest(ERROR_KEY, INVALID_MESSAGE_TYPE_NULL))
                .post(new DepositRequest("sd", MAX_AMOUNT));

        assertEquals(0, beforeBalance.compareTo(getAccountBalance()));
    }

    @Test
    public void depositWithNullAccountIdDoesNotChangeBalanceTest() {
        BigDecimal before = getBalance(token, accountId);
        deposit(token, null, new BigDecimal("10"))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
        assertEquals(0, before.compareTo(getBalance(token, accountId)));
    }

    // ---------- NEGATIVE: auth / body ----------

    @Test
    public void depositWithoutTokenTest() {
        deposit(null, accountId, new BigDecimal("10"))
                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void depositWithFakeTokenTest() {
        String fake = "Basic " + Base64.getEncoder()
                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));
        deposit(fake, accountId, new BigDecimal("10"))
                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void depositWithoutBodyTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .post(BASE_URL + API_V1 + "/accounts/deposit")
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void depositWithExtraFieldsTest() {
        // лишние поля должны игнорироваться, запрос валиден и баланс растёт
        BigDecimal before = getBalance(token, accountId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .body("""
                        {
                          "accountId": %d,
                          "amount": 10,
                          "hack": "yes"
                        }
                        """.formatted(accountId))
                .post(BASE_URL + API_V1 + "/accounts/deposit")
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, before.add(new BigDecimal("10"))
                .compareTo(getBalance(token, accountId)));
    }
}