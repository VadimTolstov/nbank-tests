package requests.skelethon.step;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import generators.RandomModelGenerator;
import lombok.NonNull;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import org.jetbrains.annotations.Nullable;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.GetUsersRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

import java.util.List;

public class AdminStep {

    public static CreateUserJsonRequest createUser() {
        CreateUserJsonRequest user = RandomModelGenerator.generate(CreateUserJsonRequest.class);
        createUser(user);
        return user;
    }

    public static CreateUserJsonResponse createUser(@NonNull CreateUserJsonRequest user) {
        return (CreateUserJsonResponse) new ValidatedCrudRequester<>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.CREATED
        ).post(user);
    }

    public static List<CreateUserJsonResponse> getUsers() {
        return new GetUsersRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS_LIST,
                ResponseSpecs.OK).getEndpoint();
    }

    public static @Nullable CreateUserJsonResponse getUserById(long id) {
        return getUsers().stream()
                .filter(user -> user.id() == id)
                .findFirst()
                .orElse(null);
    }

    public static @Nullable CreateUserJsonResponse getUserByUsername(@NonNull String username) {
        return getUsers().stream()
                .filter(user -> user.username().equals(username))
                .findFirst()
                .orElse(null);
    }
}
