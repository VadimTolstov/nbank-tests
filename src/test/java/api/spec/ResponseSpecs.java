package api.spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

/**
 * Стратегии валидации ответа.
 *
 * <p>Константы — типовые статусы (immutable, thread-safe, без аллокации
 * на каждый вызов). Фабрики — параметризованные проверки (код + тело).
 *
 * <p><b>Про тело ошибки.</b> Spring Boot / Bean Validation возвращает
 * ошибки валидации в виде {@code Map<String, List<String>>}:
 * <pre>{@code
 *   { "username": ["Username must be between 3 and 15 characters"] }
 * }</pre>
 * То есть поле — <b>массив строк</b>, даже если правило одно. Поэтому
 * {@link #errorWithField(int, String, Object)} и
 * {@link #errorWithMessage(int, String)} принимают оба варианта —
 * одиночную строку и массив — через
 * {@link Matchers#anyOf(org.hamcrest.Matcher[])}.
 */
public final class ResponseSpecs {

    private ResponseSpecs() {
    }

    // ---------- типовые статусы ----------

    /**
     * 200 OK.
     */
    public static final ResponseSpecification OK =
            status(HttpStatus.SC_OK);

    /**
     * 201 Created.
     */
    public static final ResponseSpecification CREATED =
            status(HttpStatus.SC_CREATED);

    /**
     * 204 No Content.
     */
    public static final ResponseSpecification NO_CONTENT =
            status(HttpStatus.SC_NO_CONTENT);

    /**
     * 400 Bad Request.
     */
    public static final ResponseSpecification BAD_REQUEST =
            status(HttpStatus.SC_BAD_REQUEST);

    /**
     * 401 Unauthorized.
     */
    public static final ResponseSpecification UNAUTHORIZED =
            status(HttpStatus.SC_UNAUTHORIZED);

    /**
     * 403 Forbidden.
     */
    public static final ResponseSpecification FORBIDDEN =
            status(HttpStatus.SC_FORBIDDEN);

    /**
     * 404 Not Found.
     */
    public static final ResponseSpecification NOT_FOUND =
            status(HttpStatus.SC_NOT_FOUND);

    // ---------- параметризованные ----------

    /**
     * Ожидает код {@code status} и значение {@code value} в теле
     * по пути {@code key}.
     *
     * <p>Работает и с одиночной строкой, и с массивом строк:
     * <pre>{@code
     *   { "message":  "Bad Request" }              // → equalTo
     *   { "username": ["must be 3..15 chars"] }    // → hasItem
     * }</pre>
     *
     * @param status ожидаемый HTTP-статус
     * @param key    JSON-путь поля в теле (например, {@code "username"})
     * @param value  ожидаемое значение (строка)
     * @return immutable {@link ResponseSpecification}
     */
    public static ResponseSpecification errorWithField(int status, String key, Object value) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectBody(key, Matchers.anyOf(
                        Matchers.equalTo(value),
                        Matchers.hasItem(value)
                ))
                .build();
    }

    /**
     * Ожидает код {@code status} и сообщение {@code message} в поле
     * {@code message} тела. Поле может быть как строкой, так и массивом.
     *
     * @param status  ожидаемый HTTP-статус (обычно 400/401/403)
     * @param message ожидаемое значение поля {@code message}
     * @return immutable {@link ResponseSpecification}
     */
    public static ResponseSpecification errorWithMessage(int status, String message) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectBody("message", Matchers.anyOf(
                        Matchers.equalTo(message),
                        Matchers.hasItem(message)
                ))
                .build();
    }

    // ---------- внутренний билдер ----------

    private static ResponseSpecification status(int code) {
        return new ResponseSpecBuilder().expectStatusCode(code).build();
    }
}