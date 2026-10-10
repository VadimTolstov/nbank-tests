package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AdminEndpoints;
import api.endpoint.CustomerEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import lombok.NonNull;
import models.rest.*;
import org.jetbrains.annotations.Nullable;
import service.CustomerClient;

import java.util.List;

public class CustomerApiClient implements CustomerClient, RequestExecutor {
    private static final Config CFG = Config.getInstance();

    private final RestClient restClient;

    public CustomerApiClient() {
        this.restClient = new RestClient.EmptyRestClient(
                CFG.nbankUrl(),
                CFG.apiBasePathV1(),
                false,
                LogDetail.ALL
        );
    }

    @Override
    public CustomerProfileJsonResponse getProfile(@NonNull String username) {
        return get(restClient.authRequest(username),
                CustomerEndpoints.GET_PROFILE,
                CustomerProfileJsonResponse.class
        );
    }

    @Override
    public List<CustomerAccountJson> getAccounts(@NonNull String username) {
        return get(restClient.authRequest(username),
                CustomerEndpoints.GET_ACCOUNTS,
                ResponseSpecs.OK,
                new TypeRef<>() {
                }
        );
    }

    @Override
    public @Nullable CustomerAccountJson getAccountById(@NonNull String username, long accountId) {
        return getAccounts(username).stream()
                .filter(account -> account.id() == accountId)
                .findFirst()
                .orElse(null);
    }


    @Override
    public UpdateUserNameResponse updateUserProfileName(@NonNull String username, @NonNull UpdateUserNameRequest name) {
        return put(restClient.authRequest(username),
                CustomerEndpoints.UPDATE_PROFILE,
                name,
                UpdateUserNameResponse.class
        );
    }

    public void updateNameExpectingError(@NonNull String username,
                                         @NonNull String rawJson,
                                          @NonNull String errorField,
                                          @NonNull String message) {
        executeVoid(
                restClient.authRequest(username),
                ResponseSpecs.errorWithField(400, errorField, message),
                s -> s.contentType(ContentType.JSON)
                        .body(rawJson)
                        .put(CustomerEndpoints.UPDATE_PROFILE)
        );
    }

}
