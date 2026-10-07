package tests;

import api.ApiLimits;
import jupiter.annotation.Account;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.TestData;
import models.rest.CreateUserJsonResponse;
import models.rest.TransferJson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import service.AccountsClient;
import service.CustomerClient;
import service.api.AccountsApiClient;
import service.api.CustomerApiClient;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static api.ApiLimits.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RestTest
public class TransferTest {
    private final AccountsClient accountsClient = new AccountsApiClient();
    private final CustomerClient customerClient = new CustomerApiClient();
//    private CreateUserJsonRequest firstUser;
//    private Long senderAccountIdFirstUser;
//
//
//    // ---------- asserts ----------
//
//    private void assertBalanceUnchanged(CreateUserJsonRequest user,
//                                        Long accountId,
//                                        BigDecimal before,
//                                        String message) {
//        assertEquals(0, before.compareTo(getBalance(user, accountId)), message);
//    }
//
//    /**
//     * Общий шаблон позитивного перевода:
//     * снимаем before → делаем transfer → проверяем "списалось/зачислилось".
//     */
//    private void assertSuccessfulTransfer(CreateUserJsonRequest sender,
//                                          Long senderAccountId,
//                                          CreateUserJsonRequest receiver,
//                                          Long receiverAccountId,
//                                          BigDecimal amount) {
//        BigDecimal senderBefore = getBalance(sender, senderAccountId);
//        BigDecimal receiverBefore = getBalance(receiver, receiverAccountId);
//
//        transfer(sender, ResponseSpecs.requestReturnsOK(),
//                new TransferJson(senderAccountId, receiverAccountId, amount));
//
//        assertEquals(0, senderBefore.subtract(amount)
//                        .compareTo(getBalance(sender, senderAccountId)),
//                "С отправителя должно списаться " + amount);
//        assertEquals(0, receiverBefore.add(amount)
//                        .compareTo(getBalance(receiver, receiverAccountId)),
//                "Получателю должно зачислиться " + amount);
//    }

    // ---------- setup ----------

//    @BeforeEach
//    public void setUp() {
//        firstUser = freshUser();
//        createUser(firstUser);
//        senderAccountIdFirstUser = createAccount(firstUser).getId();
//    }

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
                    @User(username = "user1", accounts = {@Account(balance = ApiLimits.TRANSFER_MAX_STR)}),
                    @User(username = "user2", randomAccounts = 1)
            }
    )
    @ApiLogin
    @ParameterizedTest
    @MethodSource("validTransferAmounts")
    public void transferValidAmountToSelfAccountTest(BigDecimal amountSent, BigDecimal remainingAmount, TestData data) {
        CreateUserJsonResponse firstUser = data.requireByUsername("user1");
        CreateUserJsonResponse secondUser = data.requireByUsername("user2");

        Long firstAccountId = firstUser.accounts().getFirst().id();
        Long secondAccountId = secondUser.accounts().getFirst().id();
        accountsClient.transfer(firstUser.username(), new TransferJson(firstAccountId, secondAccountId, amountSent, ""));
        assertAll(
                () -> assertThat(customerClient.getAccountById(firstUser.username(), firstAccountId).balance()).isEqualByComparingTo(remainingAmount),
                () -> assertThat(customerClient.getAccountById(secondUser.username(), secondAccountId).balance()).isEqualByComparingTo(amountSent)
        );
    }

//    /**
//     * Перевод 0.02 не себе при балансе 0.03.
//     */
//    @Test
//    public void transferFractionalToForeignTest() {
//        BigDecimal balance = new BigDecimal("0.03");
//        BigDecimal amount = new BigDecimal("0.02");
//
//        fillBalance(firstUser, senderAccountIdFirstUser, balance);
//
//        UserWithAccount second = freshUserWithAccount();
//
//        assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
//                second.user(), second.accountId(), amount);
//    }
//
//    /**
//     * Последовательный перевод двух сумм A→B.
//     */
//    @Test
//    public void transferSequentiallySamePairTest() {
//        BigDecimal deposit = DEPOSIT_MAX.multiply(BigDecimal.TEN);
//        fillBalance(firstUser, senderAccountIdFirstUser, deposit);
//
//        UserWithAccount second = freshUserWithAccount();
//
//        repeat(2, () -> {
//            assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
//                    second.user(), second.accountId(), DEPOSIT_MAX);
//        });
//    }
//
//    /**
//     * Баланс не меняется при переводе на тот же счёт.
//     */
//    @Test
//    public void transferToSameAccountKeepsBalanceTest() {
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//
//        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);
//
//        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
//                new TransferJson(senderAccountIdFirstUser,
//                        senderAccountIdFirstUser, DEPOSIT_MAX));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, before,
//                "Баланс не должен меняться при переводе на тот же счёт");
//    }
//
//    /**
//     * Цепочка A→B→C: A переводит B, затем B переводит C.
//     */
//    @Test
//    public void transferSequentiallyBetweenAccountsTest() {
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//
//        UserWithAccount second = freshUserWithAccount();
//        UserWithAccount third = freshUserWithAccount();
//
//        BigDecimal aBefore = getBalance(firstUser, senderAccountIdFirstUser);
//        BigDecimal bBefore = getBalance(second.user(), second.accountId());
//        BigDecimal cBefore = getBalance(third.user(), third.accountId());
//
//        // A -> B
//        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
//                new TransferJson(senderAccountIdFirstUser, second.accountId(), DEPOSIT_MAX));
//
//        // B -> C
//        transfer(second.user(), ResponseSpecs.requestReturnsOK(),
//                new TransferJson(second.accountId(), third.accountId(), DEPOSIT_MAX));
//
//        assertEquals(0, aBefore.subtract(DEPOSIT_MAX)
//                        .compareTo(getBalance(firstUser, senderAccountIdFirstUser)),
//                "A потерял " + DEPOSIT_MAX);
//        assertEquals(0, bBefore.compareTo(getBalance(second.user(), second.accountId())),
//                "B в итоге не изменился (получил и отдал " + DEPOSIT_MAX + ")");
//        assertEquals(0, cBefore.add(DEPOSIT_MAX)
//                        .compareTo(getBalance(third.user(), third.accountId())),
//                "C получил " + DEPOSIT_MAX);
//    }
//
//    // ---------- NEGATIVE: границы ----------
//
//    public static Stream<Arguments> transferInvalidData() {
//        return Stream.of(
//                Arguments.of("10000.01", ResponseSpecs.transferLimitExceeded()),
//                Arguments.of("0.00", ResponseSpecs.transferIsInvalid()),
//                Arguments.of("-0.01", ResponseSpecs.transferIsInvalid())
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("transferInvalidData")
//    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(String amount, ResponseSpecification responseSpecs) {
//        BigDecimal invalidAmount = new BigDecimal(amount);
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//
//        Long selfAccountIdFirstUser = createAccount(firstUser).getId();
//
//        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
//        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);
//
//        transfer(firstUser,
//                responseSpecs,
//                new TransferJson(senderAccountIdFirstUser, selfAccountIdFirstUser, invalidAmount));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
//                "Баланс отправителя не должен измениться");
//        assertBalanceUnchanged(firstUser, selfAccountIdFirstUser, receiverBefore,
//                "Баланс получателя не должен измениться");
//    }
//
//    // ---------- NEGATIVE: превышение баланса ----------
//
//    /**
//     * Перевод 10000 не себе при балансе 5000.
//     */
//    @Test
//    public void transferInsufficientFundsToForeignTest() {
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//
//        UserWithAccount second = freshUserWithAccount();
//
//        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
//        BigDecimal receiverBefore = getBalance(second.user(), second.accountId());
//
//        transfer(firstUser,
//                ResponseSpecs.transferIsInvalid(),
//                new TransferJson(senderAccountIdFirstUser,
//                        second.accountId(),
//                        DEPOSIT_MAX.multiply(BigDecimal.TWO)));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
//                "Баланс отправителя не должен измениться");
//        assertBalanceUnchanged(second.user(), second.accountId(), receiverBefore,
//                "Баланс получателя не должен измениться");
//    }
//
//    // ---------- NEGATIVE: чужой / несуществующий счёт ----------
//
//    /**
//     * Перевод денег с чужого счёта на свой.
//     */
//    @Test
//    public void transferFromForeignAccountDoesNotChangeBalancesTest() {
//        UserWithAccount second = freshUserWithAccount();
//        fillBalance(second.user(), second.accountId(), DEPOSIT_MAX);
//
//        BigDecimal ourBefore = getBalance(firstUser, senderAccountIdFirstUser);
//        BigDecimal foreignBefore = getBalance(second.user(), second.accountId());
//
//        transfer(firstUser,
//                ResponseSpecs.requestReturnsForbidden(),
//                new TransferJson(second.accountId(), senderAccountIdFirstUser, DEPOSIT_MAX));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, ourBefore,
//                "Баланс нашего счёта не должен измениться");
//        assertBalanceUnchanged(second.user(), second.accountId(), foreignBefore,
//                "Баланс чужого счёта не должен измениться");
//    }
//
//    /**
//     * Перевод с несуществующего счёта.
//     */
//    @Test
//    public void transferFromNonExistentAccountDoesNotChangeBalancesTest() {
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//
//        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);
//
//        transfer(firstUser,
//                ResponseSpecs.requestReturnsForbidden(),
//                new TransferJson(NOT_EXIST_ACCOUNT_ID, senderAccountIdFirstUser, DEPOSIT_MAX));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, before,
//                "Баланс не должен измениться при переводе с несуществующего счёта");
//    }
//
//    // ---------- NEGATIVE: auth ----------
//
//    public static Stream<Arguments> invalidAuthSpecs() {
//        return Stream.of(
//                Arguments.of(RequestSpecs.invalidTokenSpec(null), "без токена"),
//                Arguments.of(RequestSpecs.invalidTokenSpec(RandomData.getFakeToken()), "поддельный токен")
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("invalidAuthSpecs")
//    public void transferWithInvalidAuthDoesNotChangeBalancesTest(RequestSpecification invalidSpec, String caseName) {
//        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
//        Long selfAccountIdFirstUser = createAccount(firstUser).getId();
//
//        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
//        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);
//
//        transfer(invalidSpec, ResponseSpecs.requestReturnsUnauthorizedRequest(),
//                new TransferJson(senderAccountIdFirstUser, selfAccountIdFirstUser, DEPOSIT_MAX));
//
//        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
//                caseName + ": с баланса отправителя не должно списаться");
//        assertBalanceUnchanged(firstUser, selfAccountIdFirstUser, receiverBefore,
//                caseName + ": баланс получателя не должен измениться");
//    }
}