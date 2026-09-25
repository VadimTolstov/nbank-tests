package api.spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

/**
 * Стратегии валидации ответа.
 *
 * <p><b>Зачем.</b> Отделяют «что ожидаем от ответа» от «как выполняем запрос»
 * (это делает {@link api.core.RequestExecutor}). Одна стратегия — один
 * сценарий: успех, ошибка валидации, неавторизованный доступ и т.д.
 *
 * <p><b>Почему константы, а не фабрики.</b> {@link ResponseSpecification}
 * immutable и thread-safe, поэтому константы можно спокойно переиспользовать
 * между вызовами и потоками — без аллокации объекта на каждый запрос.
 * Фабрики оставлены только для параметризованных случаев, где значение
 * известно в рантайме (сообщение об ошибке, конкретное поле).
 *
 * <p><b>Что здесь НЕТ.</b> Уровень логирования ответа. Логирование — часть
 * транспорта, оно настраивается в {@link api.core.RestClient} и включается
 * автоматически только при падении валидации.
 *
 * <p><b>Использование.</b> Явно — второй/третий параметр в
 * {@link api.core.RequestExecutor}, неявно — дефолт {@link #OK}.
 *
 * @see api.core.RequestExecutor
 * @see api.core.RestClient
 */
public final class ResponseSpecs {

    private ResponseSpecs() {
        // utility-класс, инстанцирование запрещено
    }

    /** 200 OK. Дефолт для {@code GET}/{@code POST}/{@code PATCH} в {@link api.core.RequestExecutor}. */
    public static final ResponseSpecification OK =
            status(HttpStatus.SC_OK);

    /** 201 Created. */
    public static final ResponseSpecification CREATED =
            status(HttpStatus.SC_CREATED);

    /** 204 No Content. Дефолт для {@code DELETE}. */
    public static final ResponseSpecification NO_CONTENT =
            status(HttpStatus.SC_NO_CONTENT);

    /** 400 Bad Request. */
    public static final ResponseSpecification BAD_REQUEST =
            status(HttpStatus.SC_BAD_REQUEST);

    /** 401 Unauthorized. */
    public static final ResponseSpecification UNAUTHORIZED =
            status(HttpStatus.SC_UNAUTHORIZED);

    /** 403 Forbidden. */
    public static final ResponseSpecification FORBIDDEN =
            status(HttpStatus.SC_FORBIDDEN);

    /** 404 Not Found. */
    public static final ResponseSpecification NOT_FOUND =
            status(HttpStatus.SC_NOT_FOUND);

    /**
     * Спека «только код статуса».
     *
     * @param code ожидаемый HTTP-статус
     * @return immutable {@link ResponseSpecification}
     */
    private static ResponseSpecification status(int code) {
        return new ResponseSpecBuilder().expectStatusCode(code).build();
    }

    /**
     * Ошибка с ожидаемым сообщением в поле {@code message}.
     *
     * @param status  ожидаемый HTTP-статус (обычно 400/403/404)
     * @param message ожидаемое значение поля {@code message} в теле
     * @return immutable {@link ResponseSpecification}
     */
    public static ResponseSpecification errorWithMessage(int status, String message) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectBody("message", Matchers.equalTo(message))
                .build();
    }

    /**
     * Ошибка с проверкой произвольного поля в теле.
     *
     * @param status ожидаемый HTTP-статус
     * @param key    JSON-путь поля в теле (например, {@code "error.code"})
     * @param value  ожидаемое значение
     * @return immutable {@link ResponseSpecification}
     */
    public static ResponseSpecification errorWithField(int status, String key, Object value) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectBody(key, Matchers.equalTo(value))
                .build();
    }
}