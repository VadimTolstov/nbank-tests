package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest extends BaseTest {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000.00");

    private UserRequest firsUser;
    private Long senderAccountIdFirstUser;    // наш основной аккаунт

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
        return accountsOf(user).stream().
                filter(a -> a.getId().equals(accountId))
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

//    private void assertBalanceUnchanged(UserRequest user, BigDecimal before, String msg) {
//        assertEquals(0, before.compareTo(balanceOf(user)), msg);
//    }


    @BeforeEach
    public void setUp() {
        firsUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(firsUser);
        senderAccountIdFirstUser = createAccount(firsUser).getId();
    }

    // ---------- helpers ----------

    private void fillBalance(UserRequest user, Long accountId, BigDecimal total) {
        BigDecimal left = total;
        while (left.signum() > 0) {
            BigDecimal part = left.min(MAX_AMOUNT);
            addDeposit(user, ResponseSpecs.requestReturnsOK(), accountId, part);
            left = left.subtract(part);
        }
    }

    private void transfer(UserRequest user, ResponseSpecification responseSpecification, TransferRequest toTransfer) {
        new TransferRequester(authUser(user), responseSpecification)
                .post(toTransfer);
    }

    // ---------- POSITIVE:  ----------
    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10000", "9999.99"})
    public void transferToForeignTest(String amount) {
        BigDecimal deposit = new BigDecimal(amount);
        fillBalance(firsUser, senderAccountIdFirstUser, deposit);
        Long selfAccountIdFirstUser = createAccount(firsUser).getId();

        BigDecimal senderBefore = getBalance(firsUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(firsUser, selfAccountIdFirstUser);

        transfer(firsUser,
                ResponseSpecs.requestReturnsOK(),
                new TransferRequest(
                        senderAccountIdFirstUser,
                        selfAccountIdFirstUser,
                        deposit)
        );

        assertEquals(0, senderBefore.subtract(deposit)
                        .compareTo(getBalance(firsUser, senderAccountIdFirstUser)),
                "С отправителя должно списаться " + deposit);
        assertEquals(0, receiverBefore.add(deposit)
                        .compareTo(getBalance(firsUser, selfAccountIdFirstUser)),
                "Получателю должно зачислиться " + deposit);
    }


    /**
     * #3 Перевод 0.02 себе при балансе 0.03
     */
    @Test
    public void transferFractionalToSelfTest() {
        BigDecimal deposit = new BigDecimal("0.03");
        fillBalance(firsUser, senderAccountIdFirstUser, deposit);
        UserRequest secondUser = UserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();

        createUser(secondUser);
        Long accountIdSecondUser = createAccount(secondUser).getId();

        BigDecimal senderBefore = getBalance(firsUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(secondUser, accountIdSecondUser);

        transfer(firsUser,
                ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountIdFirstUser,
                        accountIdSecondUser,
                        new BigDecimal("0.02")
                )
        );

        assertEquals(0, senderBefore.subtract(new BigDecimal("0.02"))
                        .compareTo(getBalance(firsUser, senderAccountIdFirstUser)),
                "С отправителя списалось 0.02, осталось 0.01");
        assertEquals(0, receiverBefore.add(new BigDecimal("0.02"))
                        .compareTo(getBalance(secondUser, accountIdSecondUser)),
                "Получателю зачислилось 0.02");
    }
//
//
//
//    /**
//     * #5 Перевод 3222 не себе при балансе 23263
//     */
//    @Test
//    public void transferPartToForeignTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("23263"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, senderAccountId, foreignAccountId, new BigDecimal("3222"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//
//        assertEquals(0, senderBefore.subtract(new BigDecimal("3222"))
//                .compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, receiverBefore.add(new BigDecimal("3222"))
//                .compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
//
//    /**
//     * #6 Баланс не меняется при переводе на тот же счёт
//     */
//    @Test
//    public void transferToSameAccountKeepsBalanceTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("10000"));
//        BigDecimal before = getBalance2(token, senderAccountId);
//
//        transfer3(token, senderAccountId, senderAccountId, new BigDecimal("10000"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//
//        assertEquals(0, before.compareTo(getBalance2(token, senderAccountId)),
//                "При переводе самому себе баланс не должен меняться");
//    }
//
//    /**
//     * #7 Последовательный перевод двух сумм A→B
//     */
//    @Test
//    public void transferSequentiallySamePairTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("20000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, senderAccountId, foreignAccountId, new BigDecimal("10000"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//        transfer3(token, senderAccountId, foreignAccountId, new BigDecimal("5670"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//
//        assertEquals(0, senderBefore.subtract(new BigDecimal("15670"))
//                .compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, receiverBefore.add(new BigDecimal("15670"))
//                .compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
//
//    /**
//     * #8 Цепочка A→B→C
//     */
//    @Test
//    public void transferSequentiallyBetweenAccountsTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("10000"));
//
//        BigDecimal aBefore = getBalance2(token, senderAccountId);
//        BigDecimal bBefore = getBalance2(token, selfAccountId);
//        BigDecimal cBefore = getBalance2(foreignToken, foreignAccountId);
//
//        // A -> B
//        transfer3(token, senderAccountId, selfAccountId, new BigDecimal("10000"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//        // B -> C
//        transfer3(token, selfAccountId, foreignAccountId, new BigDecimal("10000"))
//                .then().assertThat().statusCode(HttpStatus.SC_OK);
//
//        assertEquals(0, aBefore.subtract(new BigDecimal("10000"))
//                        .compareTo(getBalance2(token, senderAccountId)),
//                "A потерял 10000");
//        assertEquals(0, bBefore.compareTo(getBalance2(token, selfAccountId)),
//                "B в итоге не заработал и не потерял");
//        assertEquals(0, cBefore.add(new BigDecimal("10000"))
//                        .compareTo(getBalance2(foreignToken, foreignAccountId)),
//                "C получил 10000");
//    }
//
//    // ---------- NEGATIVE: границы ----------
//
//    @ParameterizedTest
//    @ValueSource(strings = {"10000.01", "0", "-0.01"})
//    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(String amount) {
//        fillBalance(senderAccountId, token, new BigDecimal("10000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, senderAccountId, foreignAccountId, new BigDecimal(amount))
//                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
//
//        assertEquals(0, senderBefore.compareTo(getBalance2(token, senderAccountId)),
//                "Баланс отправителя не должен измениться");
//        assertEquals(0, receiverBefore.compareTo(getBalance2(foreignToken, foreignAccountId)),
//                "Баланс получателя не должен измениться");
//    }
//
//    // ---------- NEGATIVE: превышение баланса ----------
//
//    /**
//     * #4 Перевод 8000 не себе при балансе 5000
//     */
//    @Test
//    public void transferInsufficientFundsToForeignTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, senderAccountId, foreignAccountId, new BigDecimal("8000"))
//                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
//
//        assertEquals(0, senderBefore.compareTo(getBalance2(token, senderAccountId)),
//                "Баланс отправителя не должен измениться");
//        assertEquals(0, receiverBefore.compareTo(getBalance2(foreignToken, foreignAccountId)),
//                "Баланс получателя не должен измениться");
//    }
//
//    /**
//     * #5 Перевод 8000 себе при балансе 5000
//     */
//    @Test
//    public void transferInsufficientFundsToSelfTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal selfBefore = getBalance2(token, selfAccountId);
//
//        transfer3(token, senderAccountId, selfAccountId, new BigDecimal("8000"))
//                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
//
//        assertEquals(0, senderBefore.compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, selfBefore.compareTo(getBalance2(token, selfAccountId)));
//    }
//
//    // ---------- NEGATIVE: чужой/несуществующий счёт ----------
//
//    /**
//     * #6 Перевод денег с чужого счёта на свой
//     */
//    @Test
//    public void transferFromForeignAccountDoesNotChangeBalancesTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal ourBefore = getBalance2(token, senderAccountId);
//        BigDecimal foreignBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, foreignAccountId, senderAccountId, new BigDecimal("1000"))
//                .then().assertThat()
//                .statusCode(Matchers.anyOf(
//                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
//                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));
//
//        assertEquals(0, ourBefore.compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, foreignBefore.compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
//
//    /**
//     * #7 Перевод с несуществующего счёта
//     */
//    @Test
//    public void transferFromNonExistentAccountDoesNotChangeBalancesTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal ourBefore = getBalance2(token, senderAccountId);
//        BigDecimal foreignBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(token, 999_999L, senderAccountId, new BigDecimal("100"))
//                .then().assertThat()
//                .statusCode(Matchers.anyOf(
//                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
//                        Matchers.equalTo(HttpStatus.SC_NOT_FOUND),
//                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));
//
//        assertEquals(0, ourBefore.compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, foreignBefore.compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
//
//    // ---------- NEGATIVE: auth ----------
//
//    /**
//     * #8 Поддельный токен
//     */
//    @Test
//    public void transferWithFakeTokenDoesNotChangeBalancesTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        String fake = "Basic " + Base64.getEncoder()
//                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));
//
//        transfer3(fake, senderAccountId, foreignAccountId, new BigDecimal("100"))
//                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
//
//        assertEquals(0, senderBefore.compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, receiverBefore.compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
//
//    /**
//     * #9 Без токена
//     */
//    @Test
//    public void transferWithoutTokenDoesNotChangeBalancesTest() {
//        fillBalance(senderAccountId, token, new BigDecimal("5000"));
//
//        BigDecimal senderBefore = getBalance2(token, senderAccountId);
//        BigDecimal receiverBefore = getBalance2(foreignToken, foreignAccountId);
//
//        transfer3(null, senderAccountId, foreignAccountId, new BigDecimal("100"))
//                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
//
//        assertEquals(0, senderBefore.compareTo(getBalance2(token, senderAccountId)));
//        assertEquals(0, receiverBefore.compareTo(getBalance2(foreignToken, foreignAccountId)));
//    }
}