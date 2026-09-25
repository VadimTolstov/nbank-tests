package service;

import lombok.NonNull;
import models.rest.*;

public interface AccountsClient {

    /**
     * Создаёт аккаунт.
     *
     * @param userJson данные пользователя.
     */
    CustomerAccountJson createAccount(@NonNull CreateUserJsonRequest userJson);

    /**
     * Переводит деньги с одного аккаунта на другой аккаунт.
     *
     * @param transferJson данные перевода.
     */
    TransferJson transfer(@NonNull TransferJson transferJson);

    /**
     * Пополняет аккаунт пользователя.
     *
     * @param depositJsonRequest данные перевода.
     */
    DepositJsonResponse deposit(@NonNull DepositJsonRequest depositJsonRequest);
}