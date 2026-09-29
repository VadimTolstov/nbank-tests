package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.CustomerEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.LogDetail;
import lombok.NonNull;
import models.rest.CustomerAccountJson;
import models.rest.CustomerProfileJsonResponse;
import models.rest.UpdateUserNameRequest;
import models.rest.UpdateUserNameResponse;
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
    public UpdateUserNameResponse updateUserProfileName(@NonNull String username, @NonNull UpdateUserNameRequest name) {
        return put(restClient.authRequest(username),
                CustomerEndpoints.UPDATE_PROFILE,
                name,
                UpdateUserNameResponse.class
        );
    }
}
