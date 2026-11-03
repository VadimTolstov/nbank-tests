package service.api;

import api.core.AuthContext;
import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AuthEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;
import lombok.NonNull;
import models.rest.CreateUserJsonRequest;
import service.AuthClient;

public class AuthApiClient implements AuthClient, RequestExecutor {
    private static final Config CFG = Config.getInstance();

    private final RestClient restClient;

    public AuthApiClient() {
        this.restClient = new RestClient.EmptyRestClient(
                CFG.nbankUrl(),
                CFG.apiBasePathV1(),
                false,
                LogDetail.ALL
        );
    }

    @Override
    public String authUser(@NonNull String username, @NonNull String password) {
        Response response = postForResponse(
                restClient.request(),
                AuthEndpoints.AUTH_USER,
                CreateUserJsonRequest.builder().username(username).password(password).build(),
                ResponseSpecs.OK
        );

        String token = header(response, "Authorization");
        AuthContext.put(username, token);
        return token;
    }
}
