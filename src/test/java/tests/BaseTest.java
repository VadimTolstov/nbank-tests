package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;

import static api.ApiLimits.DEPOSIT_MAX;

public abstract class BaseTest {
    protected static final Long NOT_EXIST_ACCOUNT_ID = 999_999_999L;

    protected SoftAssertions softly;

    @BeforeEach
    public void setupTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest() {
        softly.assertAll();
    }

    protected record UserWithAccount(CreateUserJsonRequest user, Long accountId) {
    }

    protected RequestSpecification authUser(CreateUserJsonRequest user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    protected GetCustomerAccountsResponse accountsOf(CreateUserJsonRequest user) {
        return new GetCustomerAccountsRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
    }

    protected BigDecimal balanceOf(CreateUserJsonRequest user) {
        return accountsOf(user).stream()
                .map(CustomerAccountJson::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected BigDecimal getBalance(CreateUserJsonRequest user, Long accountId) {
        return accountsOf(user).stream()
                .filter(a -> a.getId().equals(accountId))
                .map(CustomerAccountJson::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected GetUsersJsonResponse getUsers() {
        return new GetUsersRequester(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetUsersJsonResponse.class);
    }

    protected CreateUserJsonResponse getUserById(CreateUserJsonResponse user) {
        return getUsers().stream()
                .filter(u -> u.getId().equals(user.getId()))
                .findFirst()
                .get();
    }

    protected @Nullable CreateUserJsonResponse getUserByName(String name) {
        return getUsers().stream()
                .filter(u -> name.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    protected CustomerAccountJson getAccountById(CreateUserJsonResponse user, Long accountId) {
        return getUserById(user).getAccounts()
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .get();
    }

    protected CreateUserJsonResponse createUser(CreateUserJsonRequest user) {
        return new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user)
                .extract()
                .as(CreateUserJsonResponse.class);
    }

    protected CustomerAccountJson createAccount(CreateUserJsonRequest user) {
        return new CreateAccountRequester(authUser(user), ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CustomerAccountJson.class);
    }

    protected CreateUserJsonRequest freshUser() {
        return CreateUserJsonRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();
    }

    protected UserWithAccount freshUserWithAccount() {
        CreateUserJsonRequest user = freshUser();
        createUser(user);
        return new UserWithAccount(user, createAccount(user).getId());
    }

    protected void addDeposit(RequestSpecification spec,
                              ResponseSpecification response,
                              Long accountId,
                              BigDecimal amount) {
        new AddDepositMoneyRequester(spec, response)
                .post(new DepositJsonRequest(accountId, amount));
    }

    protected void addDeposit(CreateUserJsonRequest user,
                              ResponseSpecification response,
                              Long accountId,
                              BigDecimal amount) {
        addDeposit(authUser(user), response, accountId, amount);
    }

    protected void fillBalance(CreateUserJsonRequest user, Long accountId, BigDecimal total) {
        BigDecimal left = total;
        while (left.signum() > 0) {
            BigDecimal part = left.min(DEPOSIT_MAX);
            addDeposit(user, ResponseSpecs.requestReturnsOK(), accountId, part);
            left = left.subtract(part);
        }
    }

    protected void transfer(RequestSpecification spec,
                            ResponseSpecification response,
                            TransferJson request) {
        new TransferRequester(spec, response).post(request);
    }

    protected void transfer(CreateUserJsonRequest user,
                            ResponseSpecification response,
                            TransferJson request) {
        transfer(authUser(user), response, request);
    }

    protected String depositBody(String accountIdJson, String amountJson) {
        return """
                {
                  "accountId": %s,
                  "amount": %s
                }
                """.formatted(accountIdJson, amountJson);
    }

    protected void depositRaw(CreateUserJsonRequest user, String rawBody, ResponseSpecification response) {
        new AddDepositMoneyRequester(authUser(user), response).postRaw(rawBody);
    }

    protected GetUserProfileResponse fetchProfile(CreateUserJsonRequest user) {
        return new GetProfileRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetUserProfileResponse.class);
    }

    protected void updateProfileName(CreateUserJsonRequest user, String name, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .put(new UpdateUserNameRequest(name));
    }

    protected void updateProfileRaw(CreateUserJsonRequest user, String rawBody, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .putRaw(rawBody);
    }

    protected void updateProfileNoBody(CreateUserJsonRequest user, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .putNoBody();
    }
}