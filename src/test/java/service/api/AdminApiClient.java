package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AdminEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.filter.log.LogDetail;
import lombok.NonNull;
import models.rest.AdminCredentials;
import models.rest.CreateUserJsonResponse;
import models.rest.GetUsersJsonResponse;
import models.rest.UserJson;
import service.AdminClient;

public class AdminApiClient implements AdminClient, RequestExecutor {
    private static Config CFG = Config.getInstance();

    private final RestClient restClient;

    public AdminApiClient() {
        this.restClient = new RestClient.EmptyRestClient(
                CFG.nbankUrl(),
                CFG.apiBasePathV1(),
                false,
                LogDetail.ALL
        );
    }

    @Override
    public GetUsersJsonResponse getUsers() {
        return null;
    }

    @Override
    public CreateUserJsonResponse createUsers(@NonNull UserJson userJson) {
        return post(
                restClient.authRequest(AdminCredentials.LOGIN.getValue()),
                AdminEndpoints.CREATE_USER,
                userJson,
                ResponseSpecs.CREATED,
                CreateUserJsonResponse.class
        );
    }
}
