package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AccountsEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.filter.log.LogDetail;
import lombok.NonNull;
import models.rest.*;
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
    public CustomerAccountJson createAccount(@NonNull UserJson userJson) {
        return post(restClient.authRequest(userJson.username()),
                AccountsEndpoints.CREATE_ACCOUNT,
                userJson,
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

    @Override
    public DepositJsonResponse deposit(@NonNull String username, @NonNull DepositJsonRequest depositJsonRequest) {
        return post(restClient.authRequest(username),
                AccountsEndpoints.DEPOSIT,
                depositJsonRequest,
                ResponseSpecs.OK,
                DepositJsonResponse.class
        );
    }
}
