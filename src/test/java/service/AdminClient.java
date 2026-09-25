package service;

import lombok.NonNull;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import models.rest.GetUsersJsonResponse;

public interface AdminClient {

    /**
     * Возвращает массив пользователей.
     *
     * @return массив пользователей.
     */
    GetUsersJsonResponse getUsers();

    /**
     * Создаёт пользователя.
     *
     * @param userJson данные нового пользователя.
     * @return создает пользователя.
     */
    CreateUserJsonResponse createUsers(@NonNull CreateUserJsonRequest userJson);
}