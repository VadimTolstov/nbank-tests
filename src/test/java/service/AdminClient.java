package service;

import lombok.NonNull;
import models.rest.CreateUserJsonResponse;
import models.rest.CreateUserJsonRequest;

import java.util.List;

public interface AdminClient {

    /**
     * Возвращает массив пользователей.
     *
     * @return массив пользователей.
     */
    List<CreateUserJsonResponse> getUsers();

    /**
     * Возвращает пользователя по id.
     *
     * @param id id пользователя.
     * @return пользователь по id.
     */
    CreateUserJsonResponse getUserById(long id);

    /**
     * Возвращает пользователя по username.
     *
     * @param username имя пользователя.
     * @return пользователь по имени.
     */
    CreateUserJsonResponse getUserByUsername(@NonNull String username);

    /**
     * Создаёт пользователя.
     *
     * @param createUserJsonRequest данные нового пользователя.
     * @return создает пользователя.
     */
    CreateUserJsonResponse createUsers(@NonNull CreateUserJsonRequest createUserJsonRequest);

    /**
     * Удалить пользователя по id пользователя.
     *
     * @param id данные нового пользователя.
     */
    void deleteUserById(long id);
}