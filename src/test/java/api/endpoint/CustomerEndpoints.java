package api.endpoint;

/**
 * Пути эндпоинтов Artist API. Один интерфейс — один ресурс.
 */
public interface CustomerEndpoints {

    String GET_ALL_USERS = "/admin/users";
    String CREATE_USER = GET_ALL_USERS;
    String DELETE_USER_ID = GET_ALL_USERS + "/{id}";
}