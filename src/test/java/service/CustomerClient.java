package service;

import lombok.NonNull;
import models.rest.*;

import java.util.List;

public interface CustomerClient {

    /**
     * Получает данные профиля.
     *
     */
    CustomerProfileJsonResponse getProfile();


    /**
     * Получает аккаунтов у пользователя.
     *
     */
    List<CustomerAccountJson> getAccounts();
}