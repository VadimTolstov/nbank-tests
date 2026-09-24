package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
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

    protected record UserWithAccount(CreateUserRequest user, Long accountId) {
    }

    protected RequestSpecification authUser(CreateUserRequest user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    protected GetCustomerAccountsResponse accountsOf(CreateUserRequest user) {
        return new GetCustomerAccountsRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerAccountsResponse.class);
    }

    protected BigDecimal balanceOf(CreateUserRequest user) {
        return accountsOf(user).stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected BigDecimal getBalance(CreateUserRequest user, Long accountId) {
        return accountsOf(user).stream()
                .filter(a -> a.getId().equals(accountId))
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected GetUsersResponse getUsers() {
        return new GetUsersRequester(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetUsersResponse.class);
    }

    protected CreateUserResponse getUserById(CreateUserResponse user) {
        return getUsers().stream()
                .filter(u -> u.getId().equals(user.getId()))
                .findFirst()
                .get();
    }

    protected @Nullable CreateUserResponse getUserByName(String name) {
        return getUsers().stream()
                .filter(u -> name.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    protected CustomerAccount getAccountById(CreateUserResponse user, Long accountId) {
        return getUserById(user).getAccounts()
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .get();
    }

    protected CreateUserResponse createUser(CreateUserRequest user) {
        return new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user)
                .extract()
                .as(CreateUserResponse.class);
    }

    protected CustomerAccount createAccount(CreateUserRequest user) {
        return new CreateAccountRequester(authUser(user), ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CustomerAccount.class);
    }

    protected CreateUserRequest freshUser() {
        return CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER)
                .build();
    }

    protected UserWithAccount freshUserWithAccount() {
        CreateUserRequest user = freshUser();
        createUser(user);
        return new UserWithAccount(user, createAccount(user).getId());
    }

    protected void addDeposit(RequestSpecification spec,
                              ResponseSpecification response,
                              Long accountId,
                              BigDecimal amount) {
        new AddDepositMoneyRequester(spec, response)
                .post(new DepositRequest(accountId, amount));
    }

    protected void addDeposit(CreateUserRequest user,
                              ResponseSpecification response,
                              Long accountId,
                              BigDecimal amount) {
        addDeposit(authUser(user), response, accountId, amount);
    }

    protected void fillBalance(CreateUserRequest user, Long accountId, BigDecimal total) {
        BigDecimal left = total;
        while (left.signum() > 0) {
            BigDecimal part = left.min(DEPOSIT_MAX);
            addDeposit(user, ResponseSpecs.requestReturnsOK(), accountId, part);
            left = left.subtract(part);
        }
    }

    protected void transfer(RequestSpecification spec,
                            ResponseSpecification response,
                            TransferRequest request) {
        new TransferRequester(spec, response).post(request);
    }

    protected void transfer(CreateUserRequest user,
                            ResponseSpecification response,
                            TransferRequest request) {
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

    protected void depositRaw(CreateUserRequest user, String rawBody, ResponseSpecification response) {
        new AddDepositMoneyRequester(authUser(user), response).postRaw(rawBody);
    }

    protected GetUserProfileResponse fetchProfile(CreateUserRequest user) {
        return new GetProfileRequester(authUser(user), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetUserProfileResponse.class);
    }

    protected void updateProfileName(CreateUserRequest user, String name, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .put(new UpdateUserNameRequest(name));
    }

    protected void updateProfileRaw(CreateUserRequest user, String rawBody, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .putRaw(rawBody);
    }

    protected void updateProfileNoBody(CreateUserRequest user, ResponseSpecification response) {
        new UpdateUserNameRequester(authUser(user), response)
                .putNoBody();
    }
}