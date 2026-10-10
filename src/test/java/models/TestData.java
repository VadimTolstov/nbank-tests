package models;

import lombok.NonNull;
import models.rest.CreateUserJsonResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record TestData(
        @NotNull List<CreateUserJsonResponse> usersJson
) {

    /**
     * Возвращает пользователя по username или бросает
     * {@link IllegalStateException}, если такого нет.
     *
     * @param username имя пользователя
     * @return найденный пользователь
     */
    public CreateUserJsonResponse requireByUsername(@NonNull String username) {
        return usersJson.stream()
                .filter(Objects::nonNull)
                .filter(u -> username.equals(u.username()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Пользователь '" + username + "' не найден. "
                                + "Доступные: " + usernames()));
    }

    /**
     * Возвращает {@code true}, если в TestData есть пользователь
     * с указанным username.
     */
    public boolean hasUsername(@NonNull String username) {
        return usersJson.stream().anyMatch(u -> username.equals(u.username()));
    }

    private List<String> usernames() {
        return usersJson.stream()
                .map(CreateUserJsonResponse::username)
                .toList();
    }
}
