package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AccountsEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.filter.log.LogDetail;
import lombok.NonNull;
import models.rest.*;
import org.apache.http.HttpStatus;
import service.AccountsClient;

public class AccountsApiClient implements AccountsClient, RequestExecutor {
    private static final Config CFG = Config.getInstance();

    private final RestClient restClient;

    public AccountsApiClient() {
        this.restClient = new RestClient.EmptyRestClient(
                CFG.nbankUrl(),
                CFG.apiBasePathV1(),
                false,
                LogDetail.ALL
        );
    }

    @Override
    public CustomerAccountJson createAccount(@NonNull CreateUserJsonRequest createUserJsonRequest) {
        return post(restClient.authRequest(createUserJsonRequest.username()),
                AccountsEndpoints.CREATE_ACCOUNT,
                createUserJsonRequest,
                ResponseSpecs.CREATED,
                CustomerAccountJson.class
        );
    }

    @Override
    public TransferJson transfer(@NonNull String username, @NonNull TransferJson transferJson) {
        return post(restClient.authRequest(username),
                AccountsEndpoints.TRANSFER_MONEY,
                transferJson,
                ResponseSpecs.OK,
                TransferJson.class
        );
    }

    public void performTransferExpectingError(@NonNull String username,
                                             @NonNull TransferJson transferJson,
                                             @NonNull String errorWithField,
                                             @NonNull String message) {
        executeVoid(
                restClient.authRequest(username),
                ResponseSpecs.errorWithField(HttpStatus.SC_BAD_REQUEST, errorWithField, message),
                s -> s.body(transferJson).post(AccountsEndpoints.TRANSFER_MONEY)
        );
    }

    @Override
    public DepositJsonResponse deposit(@NonNull String username, @NonNull DepositJsonRequest depositJsonRequest) {
        return post(restClient.authRequest(username),
                AccountsEndpoints.DEPOSIT,
                depositJsonRequest,
                ResponseSpecs.OK,
                DepositJsonResponse.class
        );
    }

    public void performDepositExpectingError(@NonNull String username,
                                             @NonNull DepositJsonRequest depositJsonRequest,
                                             int expectedStatus,
                                             @NonNull String errorWithField,
                                             @NonNull String message) {
        executeVoid(
                restClient.authRequest(username),
                ResponseSpecs.errorWithField(expectedStatus, errorWithField, message),
                s -> s.body(depositJsonRequest).post(AccountsEndpoints.DEPOSIT)
        );
    }
}
