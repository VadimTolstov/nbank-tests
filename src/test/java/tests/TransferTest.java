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
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest extends BaseTest {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000.00");
    private static final String INVALID_MESSAGE_LIMIT_10000 = "Transfer amount cannot exceed 10000";
    private static final String INVALID_MESSAGE_TRANSFER = "Invalid transfer: insufficient funds or invalid accounts";
    private static final String ERROR_KEY_MESSAGE = "message";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized access to account";
    private static final Long NOT_EXIST_ACCOUNT_ID = 999_999_999L;

    // ---------- state ----------

    private UserRequest firstUser;
    private Long senderAccountIdFirstUser;

    // ---------- records / value objects ----------

    private record UserWithAccount(UserRequest user, Long accountId) {
    }

    // ---------- base helpers ----------

    private RequestSpecification authUser(UserRequest user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    private GetCustomerAccountsResponse accountsOf(UserRequest user) {
        return new GetCustomerAccountsRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
    }

    private BigDecimal getBalance(UserRequest user, Long accountId) {
        return accountsOf(user).stream()
                .filter(a -> a.getId().equals(accountId))
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createUser(UserRequest user) {
        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user)
                .extract()
                .as(CreateUserResponse.class);
    }

    private CustomerAccount createAccount(UserRequest user) {
        return new CreateAccountRequester(authUser(user), ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CustomerAccount.class);
    }

    private UserRequest freshUser() {
        return UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();
    }

    private UserWithAccount freshUserWithAccount() {
        UserRequest user = freshUser();
        createUser(user);
        return new UserWithAccount(user, createAccount(user).getId());
    }

    // ---------- deposit / transfer ----------

    private void addDeposit(RequestSpecification spec,
                            ResponseSpecification response,
                            Long accountId,
                            BigDecimal amount) {
        new AddDepositMoneyRequester(spec, response)
                .post(new DepositRequest(accountId, amount));
    }

    private void addDeposit(UserRequest user,
                            ResponseSpecification response,
                            Long accountId,
                            BigDecimal amount) {
        addDeposit(authUser(user), response, accountId, amount);
    }

    private void fillBalance(UserRequest user, Long accountId, BigDecimal total) {
        BigDecimal left = total;
        while (left.signum() > 0) {
            BigDecimal part = left.min(MAX_AMOUNT);
            addDeposit(user, ResponseSpecs.requestReturnsOK(), accountId, part);
            left = left.subtract(part);
        }
    }

    private void transfer(RequestSpecification spec,
                          ResponseSpecification response,
                          TransferRequest request) {
        new TransferRequester(spec, response).post(request);
    }

    private void transfer(UserRequest user,
                          ResponseSpecification response,
                          TransferRequest request) {
        transfer(authUser(user), response, request);
    }

    // ---------- asserts ----------

    private void assertBalanceUnchanged(UserRequest user,
                                        Long accountId,
                                        BigDecimal before,
                                        String message) {
        assertEquals(0, before.compareTo(getBalance(user, accountId)), message);
    }

    /**
     * Общий шаблон позитивного перевода:
     * снимаем before → делаем transfer → проверяем "списалось/зачислилось".
     */
    private void assertSuccessfulTransfer(UserRequest sender,
                                          Long senderAccountId,
                                          UserRequest receiver,
                                          Long receiverAccountId,
                                          BigDecimal amount) {
        BigDecimal senderBefore = getBalance(sender, senderAccountId);
        BigDecimal receiverBefore = getBalance(receiver, receiverAccountId);

        transfer(sender, ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountId, receiverAccountId, amount));

        assertEquals(0, senderBefore.subtract(amount)
                        .compareTo(getBalance(sender, senderAccountId)),
                "С отправителя должно списаться " + amount);
        assertEquals(0, receiverBefore.add(amount)
                        .compareTo(getBalance(receiver, receiverAccountId)),
                "Получателю должно зачислиться " + amount);
    }

    // ---------- setup ----------

    @BeforeEach
    public void setUp() {
        firstUser = freshUser();
        createUser(firstUser);
        senderAccountIdFirstUser = createAccount(firstUser).getId();
    }

    // ---------- POSITIVE ----------

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10000", "9999.99"})
    public void transferValidAmountToSelfAccountTest(String amount) {
        BigDecimal deposit = new BigDecimal(amount);
        fillBalance(firstUser, senderAccountIdFirstUser, deposit);

        Long selfAccountIdFirstUser = createAccount(firstUser).getId();

        assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
                firstUser, selfAccountIdFirstUser, deposit);
    }

    /**
     * Перевод 0.02 не себе при балансе 0.03.
     */
    @Test
    public void transferFractionalToForeignTest() {
        BigDecimal balance = new BigDecimal("0.03");
        BigDecimal amount = new BigDecimal("0.02");

        fillBalance(firstUser, senderAccountIdFirstUser, balance);

        UserWithAccount second = freshUserWithAccount();

        assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
                second.user(), second.accountId(), amount);
    }

    /**
     * Последовательный перевод двух сумм A→B.
     */
    @Test
    public void transferSequentiallySamePairTest() {
        BigDecimal deposit = MAX_AMOUNT.multiply(BigDecimal.TEN);
        fillBalance(firstUser, senderAccountIdFirstUser, deposit);

        UserWithAccount second = freshUserWithAccount();

        for (int i = 1; i <= 2; i++) {
            assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
                    second.user(), second.accountId(), MAX_AMOUNT);
        }
    }

    /**
     * Баланс не меняется при переводе на тот же счёт.
     */
    @Test
    public void transferToSameAccountKeepsBalanceTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);

        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);

        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountIdFirstUser,
                        senderAccountIdFirstUser, MAX_AMOUNT));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, before,
                "Баланс не должен меняться при переводе на тот же счёт");
    }

    /**
     * Цепочка A→B→C: A переводит B, затем B переводит C.
     */
    @Test
    public void transferSequentiallyBetweenAccountsTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);

        UserWithAccount second = freshUserWithAccount();
        UserWithAccount third = freshUserWithAccount();

        BigDecimal aBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal bBefore = getBalance(second.user(), second.accountId());
        BigDecimal cBefore = getBalance(third.user(), third.accountId());

        // A -> B
        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountIdFirstUser, second.accountId(), MAX_AMOUNT));

        // B -> C
        transfer(second.user(), ResponseSpecs.requestReturnsOK(),
                new TransferRequest(second.accountId(), third.accountId(), MAX_AMOUNT));

        assertEquals(0, aBefore.subtract(MAX_AMOUNT)
                        .compareTo(getBalance(firstUser, senderAccountIdFirstUser)),
                "A потерял " + MAX_AMOUNT);
        assertEquals(0, bBefore.compareTo(getBalance(second.user(), second.accountId())),
                "B в итоге не изменился (получил и отдал " + MAX_AMOUNT + ")");
        assertEquals(0, cBefore.add(MAX_AMOUNT)
                        .compareTo(getBalance(third.user(), third.accountId())),
                "C получил " + MAX_AMOUNT);
    }

    // ---------- NEGATIVE: границы ----------

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of("10000.01", INVALID_MESSAGE_LIMIT_10000),
                Arguments.of("0.00", INVALID_MESSAGE_TRANSFER),
                Arguments.of("-0.01", INVALID_MESSAGE_TRANSFER)
        );
    }

    @ParameterizedTest
    @MethodSource("transferInvalidData")
    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(String amount, String errorMessage) {
        BigDecimal invalidAmount = new BigDecimal(amount);
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);

        Long selfAccountIdFirstUser = createAccount(firstUser).getId();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);

        transfer(firstUser,
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_MESSAGE, errorMessage),
                new TransferRequest(senderAccountIdFirstUser, selfAccountIdFirstUser, invalidAmount));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
                "Баланс отправителя не должен измениться");
        assertBalanceUnchanged(firstUser, selfAccountIdFirstUser, receiverBefore,
                "Баланс получателя не должен измениться");
    }

    // ---------- NEGATIVE: превышение баланса ----------

    /**
     * Перевод 10000 не себе при балансе 5000.
     */
    @Test
    public void transferInsufficientFundsToForeignTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);

        UserWithAccount second = freshUserWithAccount();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(second.user(), second.accountId());

        transfer(firstUser,
                ResponseSpecs.requestReturnsBadRequest(ERROR_KEY_MESSAGE, INVALID_MESSAGE_TRANSFER),
                new TransferRequest(senderAccountIdFirstUser,
                        second.accountId(),
                        MAX_AMOUNT.multiply(BigDecimal.TWO)));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
                "Баланс отправителя не должен измениться");
        assertBalanceUnchanged(second.user(), second.accountId(), receiverBefore,
                "Баланс получателя не должен измениться");
    }

    // ---------- NEGATIVE: чужой / несуществующий счёт ----------

    /**
     * Перевод денег с чужого счёта на свой.
     */
    @Test
    public void transferFromForeignAccountDoesNotChangeBalancesTest() {
        UserWithAccount second = freshUserWithAccount();
        fillBalance(second.user(), second.accountId(), MAX_AMOUNT);

        BigDecimal ourBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal foreignBefore = getBalance(second.user(), second.accountId());

        transfer(firstUser,
                ResponseSpecs.requestReturnsForbidden(ERROR_KEY_MESSAGE, UNAUTHORIZED_MESSAGE),
                new TransferRequest(second.accountId(), senderAccountIdFirstUser, MAX_AMOUNT));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, ourBefore,
                "Баланс нашего счёта не должен измениться");
        assertBalanceUnchanged(second.user(), second.accountId(), foreignBefore,
                "Баланс чужого счёта не должен измениться");
    }

    /**
     * Перевод с несуществующего счёта.
     */
    @Test
    public void transferFromNonExistentAccountDoesNotChangeBalancesTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);

        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);

        transfer(firstUser,
                ResponseSpecs.requestReturnsForbidden(ERROR_KEY_MESSAGE, UNAUTHORIZED_MESSAGE),
                new TransferRequest(NOT_EXIST_ACCOUNT_ID, senderAccountIdFirstUser, MAX_AMOUNT));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, before,
                "Баланс не должен измениться при переводе с несуществующего счёта");
    }

    // ---------- NEGATIVE: auth ----------

    public static Stream<Arguments> invalidAuthSpecs() {
        return Stream.of(
                Arguments.of(RequestSpecs.invalidTokenSpec(null), "без токена"),
                Arguments.of(RequestSpecs.invalidTokenSpec(RandomData.getFakeToken()), "поддельный токен")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAuthSpecs")
    public void transferWithInvalidAuthDoesNotChangeBalancesTest(RequestSpecification invalidSpec,
                                                                 String caseName) {
        fillBalance(firstUser, senderAccountIdFirstUser, MAX_AMOUNT);
        Long selfAccountIdFirstUser = createAccount(firstUser).getId();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);

        transfer(invalidSpec, ResponseSpecs.requestReturnsUnauthorizedRequest(),
                new TransferRequest(senderAccountIdFirstUser, selfAccountIdFirstUser, MAX_AMOUNT));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
                caseName + ": с баланса отправителя не должно списаться");
        assertBalanceUnchanged(firstUser, selfAccountIdFirstUser, receiverBefore,
                caseName + ": баланс получателя не должен измениться");
    }
}