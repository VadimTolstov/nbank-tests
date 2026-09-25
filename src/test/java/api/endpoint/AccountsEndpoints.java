package api.endpoint;

/**
 * Пути эндпоинтов Artist API. Один интерфейс — один ресурс.
 */
public interface AccountsEndpoints {

    String CREATE_ACCOUNT = "/accounts";
    String COMPLETE_PENDING_TRANSACTIONS = CREATE_ACCOUNT + "/transfers/{id}/complete";
    String TRANSFER_MONEY = CREATE_ACCOUNT + "/transfers";
    String DEPOSIT = CREATE_ACCOUNT + "/deposit";
}