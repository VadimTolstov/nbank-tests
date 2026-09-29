package service;

import lombok.NonNull;
import models.rest.*;

import java.util.List;

public interface CustomerClient {

    /**
     * Получает данные профиля.
     *
     */
    CustomerProfileJsonResponse getProfile(@NonNull String username);


    /**
     * Получает аккаунтов у пользователя.
     *
     */
    List<CustomerAccountJson> getAccounts(@NonNull String username);

    /**
     * Обновляем профиль у пользователя.
     *
     */
    UpdateUserNameResponse updateUserProfileName(@NonNull String username, @NonNull UpdateUserNameRequest name);
}