package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.TransferRequest;
import models.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static specs.ApiLimits.DEPOSIT_MAX;

public class TransferTest extends BaseTest {

    private UserRequest firstUser;
    private Long senderAccountIdFirstUser;


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
        BigDecimal deposit = DEPOSIT_MAX.multiply(BigDecimal.TEN);
        fillBalance(firstUser, senderAccountIdFirstUser, deposit);

        UserWithAccount second = freshUserWithAccount();

        for (int i = 1; i <= 2; i++) {
            assertSuccessfulTransfer(firstUser, senderAccountIdFirstUser,
                    second.user(), second.accountId(), DEPOSIT_MAX);
        }
    }

    /**
     * Баланс не меняется при переводе на тот же счёт.
     */
    @Test
    public void transferToSameAccountKeepsBalanceTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);

        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);

        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountIdFirstUser,
                        senderAccountIdFirstUser, DEPOSIT_MAX));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, before,
                "Баланс не должен меняться при переводе на тот же счёт");
    }

    /**
     * Цепочка A→B→C: A переводит B, затем B переводит C.
     */
    @Test
    public void transferSequentiallyBetweenAccountsTest() {
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);

        UserWithAccount second = freshUserWithAccount();
        UserWithAccount third = freshUserWithAccount();

        BigDecimal aBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal bBefore = getBalance(second.user(), second.accountId());
        BigDecimal cBefore = getBalance(third.user(), third.accountId());

        // A -> B
        transfer(firstUser, ResponseSpecs.requestReturnsOK(),
                new TransferRequest(senderAccountIdFirstUser, second.accountId(), DEPOSIT_MAX));

        // B -> C
        transfer(second.user(), ResponseSpecs.requestReturnsOK(),
                new TransferRequest(second.accountId(), third.accountId(), DEPOSIT_MAX));

        assertEquals(0, aBefore.subtract(DEPOSIT_MAX)
                        .compareTo(getBalance(firstUser, senderAccountIdFirstUser)),
                "A потерял " + DEPOSIT_MAX);
        assertEquals(0, bBefore.compareTo(getBalance(second.user(), second.accountId())),
                "B в итоге не изменился (получил и отдал " + DEPOSIT_MAX + ")");
        assertEquals(0, cBefore.add(DEPOSIT_MAX)
                        .compareTo(getBalance(third.user(), third.accountId())),
                "C получил " + DEPOSIT_MAX);
    }

    // ---------- NEGATIVE: границы ----------

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of("10000.01", ResponseSpecs.transferLimitExceeded()),
                Arguments.of("0.00", ResponseSpecs.transferIsInvalid()),
                Arguments.of("-0.01", ResponseSpecs.transferIsInvalid())
        );
    }

    @ParameterizedTest
    @MethodSource("transferInvalidData")
    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(String amount, ResponseSpecification responseSpecs) {
        BigDecimal invalidAmount = new BigDecimal(amount);
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);

        Long selfAccountIdFirstUser = createAccount(firstUser).getId();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);

        transfer(firstUser,
                responseSpecs,
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
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);

        UserWithAccount second = freshUserWithAccount();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(second.user(), second.accountId());

        transfer(firstUser,
                ResponseSpecs.transferIsInvalid(),
                new TransferRequest(senderAccountIdFirstUser,
                        second.accountId(),
                        DEPOSIT_MAX.multiply(BigDecimal.TWO)));

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
        fillBalance(second.user(), second.accountId(), DEPOSIT_MAX);

        BigDecimal ourBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal foreignBefore = getBalance(second.user(), second.accountId());

        transfer(firstUser,
                ResponseSpecs.requestReturnsForbidden(),
                new TransferRequest(second.accountId(), senderAccountIdFirstUser, DEPOSIT_MAX));

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
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);

        BigDecimal before = getBalance(firstUser, senderAccountIdFirstUser);

        transfer(firstUser,
                ResponseSpecs.requestReturnsForbidden(),
                new TransferRequest(NOT_EXIST_ACCOUNT_ID, senderAccountIdFirstUser, DEPOSIT_MAX));

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
    public void transferWithInvalidAuthDoesNotChangeBalancesTest(RequestSpecification invalidSpec, String caseName) {
        fillBalance(firstUser, senderAccountIdFirstUser, DEPOSIT_MAX);
        Long selfAccountIdFirstUser = createAccount(firstUser).getId();

        BigDecimal senderBefore = getBalance(firstUser, senderAccountIdFirstUser);
        BigDecimal receiverBefore = getBalance(firstUser, selfAccountIdFirstUser);

        transfer(invalidSpec, ResponseSpecs.requestReturnsUnauthorizedRequest(),
                new TransferRequest(senderAccountIdFirstUser, selfAccountIdFirstUser, DEPOSIT_MAX));

        assertBalanceUnchanged(firstUser, senderAccountIdFirstUser, senderBefore,
                caseName + ": с баланса отправителя не должно списаться");
        assertBalanceUnchanged(firstUser, selfAccountIdFirstUser, receiverBefore,
                caseName + ": баланс получателя не должен измениться");
    }
}