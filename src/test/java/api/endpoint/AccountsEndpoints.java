package api.endpoint;

/**
 * Пути эндпоинтов Artist API. Один интерфейс — один ресурс.
 */
public interface AccountsEndpoints {

    String CREATE_ACCOUNT = "accounts";
    String TRANSFER_MONEY = CREATE_ACCOUNT + "/transfer";
    String COMPLETE_PENDING_TRANSACTIONS = TRANSFER_MONEY + "/{id}/complete";
    String DEPOSIT = CREATE_ACCOUNT + "/deposit";
}
