package tests;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest extends BaseTest {

    private String userName;
    private String token;
    private long accountId;

    @BeforeEach
    public void setUp() {
        userName = randomUserName();
        createUser(userName, DEFAULT_PASSWORD);
        token = loginAndGetToken(userName, DEFAULT_PASSWORD);
        accountId = createAccount(token, userName, DEFAULT_PASSWORD);
    }

    // ---------- POSITIVE:  ----------

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.02", "4999.99", "5000"})
    public void depositValidBoundaryAmountChangesBalanceTest(String amount) {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, accountId, new BigDecimal(amount))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        BigDecimal after = getBalance(token, accountId);
        assertEquals(0, before.add(new BigDecimal(amount)).compareTo(after),
                "Баланс должен увеличиться ровно на " + amount);
    }

    // ---------- NEGATIVE: границы  ----------

    @ParameterizedTest
    @ValueSource(strings = {"5000.01", "0", "-0.01"})
    public void depositInvalidBoundaryAmountDoesNotChangeBalanceTest(String amount) {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, accountId, new BigDecimal(amount))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        BigDecimal after = getBalance(token, accountId);
        assertEquals(0, before.compareTo(after),
                "Баланс не должен меняться при невалидной сумме " + amount);
    }

    // ---------- POSITIVE: накопление ----------

    @Test
    public void depositSequentiallyAccumulatesBalanceTest() {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, accountId, new BigDecimal("5000"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);
        deposit(token, accountId, new BigDecimal("0.01"))
                .then().assertThat().statusCode(HttpStatus.SC_OK);

        assertEquals(0, before.add(new BigDecimal("5000.01"))
                .compareTo(getBalance(token, accountId)));
    }

    // ---------- NEGATIVE: невалидные типы / аккаунт ----------

    @Test
    public void depositWithNullAmountDoesNotChangeBalanceTest() {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, accountId, null)
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertEquals(0, before.compareTo(getBalance(token, accountId)));
    }

    @Test
    public void depositWithStringAmountDoesNotChangeBalanceTest() {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, accountId, "sda")
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertEquals(0, before.compareTo(getBalance(token, accountId)));
    }

    @Test
    public void depositToNonExistentAccountTest() {
        BigDecimal before = getBalance(token, accountId);

        deposit(token, 999_999_999L, new BigDecimal("10"))
                .then().assertThat()
                .statusCode(Matchers.anyOf(
                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
                        Matchers.equalTo(HttpStatus.SC_NOT_FOUND),
                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));

        assertEquals(0, before.compareTo(getBalance(token, accountId)),
                "Баланс нашего аккаунта не должен измениться");
    }

    @Test
    public void depositToForeignAccountDoesNotAffectBalancesTest() {
        // создаём второго юзера и его аккаунт
        String foreignUser = randomUserName();
        createUser(foreignUser, DEFAULT_PASSWORD);
        String foreignToken = loginAndGetToken(foreignUser, DEFAULT_PASSWORD);
        long foreignAccount = createAccount(foreignToken, foreignUser, DEFAULT_PASSWORD);

        BigDecimal ourBefore     = getBalance(token, accountId);
        BigDecimal foreignBefore = getBalance(foreignToken, foreignAccount);

        deposit(token, foreignAccount, new BigDecimal("10"))
                .then().assertThat()
                .statusCode(Matchers.anyOf(
                        Matchers.equalTo(HttpStatus.SC_BAD_REQUEST),
                        Matchers.equalTo(HttpStatus.SC_FORBIDDEN)));

        assertEquals(0, ourBefore.compareTo(getBalance(token, accountId)),
                "Наш баланс не должен измениться");
        assertEquals(0, foreignBefore.compareTo(getBalance(foreignToken, foreignAccount)),
                "Баланс чужого аккаунта не должен измениться");
    }

    @Test
    public void depositWithStringAccountIdDoesNotChangeBalanceTest() {
        BigDecimal before = getBalance(token, accountId);
        deposit(token, "da", new BigDecimal("10"))
                .then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
        assertEquals(0, before.compareTo(getBalance(token, accountId)));
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