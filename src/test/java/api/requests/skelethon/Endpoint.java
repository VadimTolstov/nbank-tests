package api.requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.rest.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
// ---------- Admin ----------

    /**
     * {@code POST /admin/users} — создать пользователя (admin).
     */
    ADMIN_USER(
            "admin/users",
            CreateUserJsonRequest.class,
            CreateUserJsonResponse.class
    ),

    /**
     * {@code GET /admin/users} — список пользователей (admin).
     */
    ADMIN_USERS_LIST(
            "admin/users",
            null,
            null
    ),

    /**
     * {@code DELETE /admin/users/{id}} — удалить пользователя (admin).
     */
    ADMIN_USER_BY_ID(
            "admin/users/{id}",
            null,
            null
    ),

    // ---------- Auth ----------

    /**
     * {@code POST /auth/login} — логин. Токен приходит в заголовке {@code Authorization}.
     */
    AUTH_LOGIN(
            "auth/login",
            CreateUserJsonRequest.class,
            null
    ),

    // ---------- Accounts ----------

    /**
     * {@code POST /accounts} — создать счёт.
     */
    ACCOUNTS(
            "accounts",
            CreateUserJsonRequest.class,
            CustomerAccountJson.class
    ),

    /**
     * {@code POST /accounts/deposit} — пополнить счёт.
     */
    ACCOUNTS_DEPOSIT(
            "accounts/deposit",
            DepositJsonRequest.class,
            DepositJsonResponse.class
    ),

    /**
     * {@code POST /accounts/transfer} — перевод между счетами.
     */
    ACCOUNTS_TRANSFER(
            "accounts/transfer",
            TransferJson.class,
            TransferJson.class
    ),

    // ---------- Customer ----------

    /**
     * {@code GET /customer/profile} — профиль текущего пользователя.
     */
    CUSTOMER_PROFILE(
            "customer/profile",
            null,
            CustomerProfileJsonResponse.class
    ),

    /**
     * {@code PUT /customer/profile} — обновить имя.
     */
    CUSTOMER_PROFILE_UPDATE(
            "customer/profile",
            UpdateUserNameRequest.class,
            UpdateUserNameResponse.class
    ),

    /**
     * {@code GET /customer/accounts} — список счетов текущего пользователя.
     */
    CUSTOMER_ACCOUNTS(
            "customer/accounts",
            null,
            CustomerAccountJson.class
    );

    private final String endpoint;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;
}
