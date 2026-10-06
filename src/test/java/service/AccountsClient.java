package service;

import lombok.NonNull;
import models.rest.*;

public interface AccountsClient {

    /**
     * Создаёт аккаунт.
     *
     * @param userJson данные пользователя.
     */
    CustomerAccountJson createAccount(@NonNull UserJson userJson);

    /**
     * Переводит деньги с одного аккаунта на другой аккаунт.
     *
     * @param transferJson данные перевода.
     */
    TransferJson transfer(@NonNull String username, @NonNull TransferJson transferJson);

    /**
     * Пополняет аккаунт пользователя.
     *
     * @param depositJsonRequest данные перевода.
     */
    DepositJsonResponse deposit(@NonNull String username,@NonNull DepositJsonRequest depositJsonRequest);
}