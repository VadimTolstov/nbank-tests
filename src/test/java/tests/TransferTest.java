package tests;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest extends BaseTest {

    private String token;
    private long senderAccountId;        // наш основной аккаунт
    private long selfAccountId;          // наш второй аккаунт («себе»)

    // чужой юзер — чтобы видеть его баланс после переводов «не себе»
    private String foreignToken;
    private long foreignAccountId;

    @BeforeEach
    public void setUp() {
        String userName = randomUserName();
        createUser(userName, DEFAULT_PASSWORD);
        token = loginAndGetToken(userName, DEFAULT_PASSWORD);
        senderAccountId = createAccount(token, userName, DEFAULT_PASSWORD);
        selfAccountId   = createAccount(token, userName, DEFAULT_PASSWORD);

        String foreignUser = randomUserName();
        createUser(foreignUser, DEFAULT_PASSWORD);
        foreignToken = loginAndGetToken(foreignUser, DEFAULT_PASSWORD);
        foreignAccountId = createAccount(foreignToken, foreignUser, DEFAULT_PASSWORD);
    }

    // ---------- helpers ----------

    private void fillBalance(long accountId, String tokenForAccount, BigDecimal total) {
        BigDecimal left = total;
        BigDecimal maxStep = new BigDecimal("5000");
        while (left.signum() > 0) {
            BigDecimal part = left.min(maxStep);
            deposit(tokenForAccount, accountId, part)
                    .then().assertThat().statusCode(HttpStatus.SC_OK);
            left = left.subtract(part);
        }
    }

    // ---------- POSITIVE:  ----------

    /** #1 Перевод максимальной суммы 10000 при балансе 10000 (не себе) */
    @Test
    public void transferMaxToForeignTest() {
        fillBalance(senderAccountId, token, new BigDecimal("10000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("10000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("10000"))
                        .compareTo(getBalance(token, senderAccountId)),
                "С отправителя должно списаться 10000");
        assertEquals(0, receiverBefore.add(new BigDecimal("10000"))
                        .compareTo(getBalance(foreignToken, foreignAccountId)),
                "Получателю должно зачислиться 10000");
    }

    /** #2 Перевод 9999.99 себе при балансе 9999.99 */
    @Test
    public void transferAlmostMaxToSelfTest() {
        fillBalance(senderAccountId, token, new BigDecimal("9999.99"));

        BigDecimal senderBefore = getBalance(token, senderAccountId);
        BigDecimal selfBefore   = getBalance(token, selfAccountId);

        transfer(token, senderAccountId, selfAccountId, new BigDecimal("9999.99"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("9999.99"))
                .compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, selfBefore.add(new BigDecimal("9999.99"))
                .compareTo(getBalance(token, selfAccountId)));
    }

    /** #3 Перевод 0.02 себе при балансе 0.03 */
    @Test
    public void transferFractionalToSelfTest() {
        deposit(token, senderAccountId, new BigDecimal("0.03"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        BigDecimal senderBefore = getBalance(token, senderAccountId);
        BigDecimal selfBefore   = getBalance(token, selfAccountId);

        transfer(token, senderAccountId, selfAccountId, new BigDecimal("0.02"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("0.02"))
                        .compareTo(getBalance(token, senderAccountId)),
                "С отправителя списалось 0.02, осталось 0.01");
        assertEquals(0, selfBefore.add(new BigDecimal("0.02"))
                        .compareTo(getBalance(token, selfAccountId)),
                "Получателю зачислилось 0.02");
    }

    /** #4 Перевод минимальной суммы 0.01 при балансе 0.01 (не себе) */
    @Test
    public void transferMinToForeignTest() {
        deposit(token, senderAccountId, new BigDecimal("0.01"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("0.01"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("0.01"))
                .compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, receiverBefore.add(new BigDecimal("0.01"))
                .compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    /** #5 Перевод 3222 не себе при балансе 23263 */
    @Test
    public void transferPartToForeignTest() {
        fillBalance(senderAccountId, token, new BigDecimal("23263"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("3222"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("3222"))
                .compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, receiverBefore.add(new BigDecimal("3222"))
                .compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    /** #6 Баланс не меняется при переводе на тот же счёт */
    @Test
    public void transferToSameAccountKeepsBalanceTest() {
        fillBalance(senderAccountId, token, new BigDecimal("10000"));
        BigDecimal before = getBalance(token, senderAccountId);

        transfer(token, senderAccountId, senderAccountId, new BigDecimal("10000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, before.compareTo(getBalance(token, senderAccountId)),
                "При переводе самому себе баланс не должен меняться");
    }

    /** #7 Последовательный перевод двух сумм A→B */
    @Test
    public void transferSequentiallySamePairTest() {
        fillBalance(senderAccountId, token, new BigDecimal("20000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("10000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);
        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("5670"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, senderBefore.subtract(new BigDecimal("15670"))
                .compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, receiverBefore.add(new BigDecimal("15670"))
                .compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    /** #8 Цепочка A→B→C */
    @Test
    public void transferSequentiallyBetweenAccountsTest() {
        fillBalance(senderAccountId, token, new BigDecimal("10000"));

        BigDecimal aBefore = getBalance(token, senderAccountId);
        BigDecimal bBefore = getBalance(token, selfAccountId);
        BigDecimal cBefore = getBalance(foreignToken, foreignAccountId);

        // A -> B
        transfer(token, senderAccountId, selfAccountId, new BigDecimal("10000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);
        // B -> C
        transfer(token, selfAccountId, foreignAccountId, new BigDecimal("10000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, aBefore.subtract(new BigDecimal("10000"))
                        .compareTo(getBalance(token, senderAccountId)),
                "A потерял 10000");
        assertEquals(0, bBefore.compareTo(getBalance(token, selfAccountId)),
                "B в итоге не заработал и не потерял");
        assertEquals(0, cBefore.add(new BigDecimal("10000"))
                        .compareTo(getBalance(foreignToken, foreignAccountId)),
                "C получил 10000");
    }

    // ---------- NEGATIVE: границы ----------

    @ParameterizedTest
    @ValueSource(strings = {"10000.01", "0", "-0.01"})
    public void transferInvalidBoundaryAmountDoesNotChangeBalancesTest(String amount) {
        fillBalance(senderAccountId, token, new BigDecimal("10000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal(amount))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertEquals(0, senderBefore.compareTo(getBalance(token, senderAccountId)),
                "Баланс отправителя не должен измениться");
        assertEquals(0, receiverBefore.compareTo(getBalance(foreignToken, foreignAccountId)),
                "Баланс получателя не должен измениться");
    }

    // ---------- NEGATIVE: превышение баланса ----------

    /** #4 Перевод 8000 не себе при балансе 5000 */
    @Test
    public void transferInsufficientFundsToForeignTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, senderAccountId, foreignAccountId, new BigDecimal("8000"))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertEquals(0, senderBefore.compareTo(getBalance(token, senderAccountId)),
                "Баланс отправителя не должен измениться");
        assertEquals(0, receiverBefore.compareTo(getBalance(foreignToken, foreignAccountId)),
                "Баланс получателя не должен измениться");
    }

    /** #5 Перевод 8000 себе при балансе 5000 */
    @Test
    public void transferInsufficientFundsToSelfTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal senderBefore = getBalance(token, senderAccountId);
        BigDecimal selfBefore   = getBalance(token, selfAccountId);

        transfer(token, senderAccountId, selfAccountId, new BigDecimal("8000"))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertEquals(0, senderBefore.compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, selfBefore.compareTo(getBalance(token, selfAccountId)));
    }

    // ---------- NEGATIVE: чужой/несуществующий счёт ----------

    /** #6 Перевод денег с чужого счёта на свой */
    @Test
    public void transferFromForeignAccountDoesNotChangeBalancesTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal ourBefore     = getBalance(token, senderAccountId);
        BigDecimal foreignBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, foreignAccountId, senderAccountId, new BigDecimal("1000"))
                .then().assertThat()
                .statusCode(Matchers.anyOf(
                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));

        assertEquals(0, ourBefore.compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, foreignBefore.compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    /** #7 Перевод с несуществующего счёта */
    @Test
    public void transferFromNonExistentAccountDoesNotChangeBalancesTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal ourBefore     = getBalance(token, senderAccountId);
        BigDecimal foreignBefore = getBalance(foreignToken, foreignAccountId);

        transfer(token, 999_999L, senderAccountId, new BigDecimal("100"))
                .then().assertThat()
                .statusCode(Matchers.anyOf(
                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
                        Matchers.equalTo(HttpStatus.SC_NOT_FOUND),
                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));

        assertEquals(0, ourBefore.compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, foreignBefore.compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    // ---------- NEGATIVE: auth ----------

    /** #8 Поддельный токен */
    @Test
    public void transferWithFakeTokenDoesNotChangeBalancesTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        String fake = "Basic " + Base64.getEncoder()
                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));

        transfer(fake, senderAccountId, foreignAccountId, new BigDecimal("100"))
                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);

        assertEquals(0, senderBefore.compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, receiverBefore.compareTo(getBalance(foreignToken, foreignAccountId)));
    }

    /** #9 Без токена */
    @Test
    public void transferWithoutTokenDoesNotChangeBalancesTest() {
        fillBalance(senderAccountId, token, new BigDecimal("5000"));

        BigDecimal senderBefore   = getBalance(token, senderAccountId);
        BigDecimal receiverBefore = getBalance(foreignToken, foreignAccountId);

        transfer(null, senderAccountId, foreignAccountId, new BigDecimal("100"))
                .then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);

        assertEquals(0, senderBefore.compareTo(getBalance(token, senderAccountId)));
        assertEquals(0, receiverBefore.compareTo(getBalance(foreignToken, foreignAccountId)));
    }
}