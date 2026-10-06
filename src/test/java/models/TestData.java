package models;

import models.rest.CreateUserJsonResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public record TestData(
        @NotNull List<CreateUserJsonResponse> usersJson
) {
}
