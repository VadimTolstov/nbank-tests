package service.api;

import api.core.RequestExecutor;
import api.core.RestClient;
import api.endpoint.AdminEndpoints;
import api.spec.ResponseSpecs;
import config.Config;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.LogDetail;
import lombok.NonNull;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonResponse;
import models.rest.UserJson;
import org.jetbrains.annotations.Nullable;
import service.AdminClient;

import java.util.List;
import java.util.Map;

public class AdminApiClient implements AdminClient, RequestExecutor {
    private static final Config CFG = Config.getInstance();

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
    public List<CreateUserJsonResponse> getUsers() {
        return get(
                restClient.authRequest(AdminConstants.LOGIN),
                AdminEndpoints.GET_ALL_USERS,
                ResponseSpecs.OK,
                new TypeRef<>() {
                }
        );
    }

    @Override
    public @Nullable CreateUserJsonResponse getUserById(long id) {
        return getUsers().stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public @Nullable CreateUserJsonResponse getUserByUsername(@NonNull String username) {
        return getUsers().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public CreateUserJsonResponse createUsers(@NonNull UserJson userJson) {
        return post(
                restClient.authRequest(AdminConstants.LOGIN),
                AdminEndpoints.CREATE_USER,
                userJson,
                ResponseSpecs.CREATED,
                CreateUserJsonResponse.class
        );
    }

    @Override
    public void deleteUserById(long id) {
        delete(restClient.authRequest(AdminConstants.LOGIN),
                AdminEndpoints.DELETE_USER_ID,
                Map.of("id", id),
                ResponseSpecs.OK
        );
    }

    public void createUsersExpectingError(@NonNull UserJson userJson,
                                          @NonNull String errorWithField,
                                          @NonNull String message) {
        executeVoid(
                restClient.authRequest(AdminConstants.LOGIN),
                ResponseSpecs.errorWithField(400, errorWithField, message),
                s -> s.body(userJson).post(AdminEndpoints.CREATE_USER)
        );
    }
}