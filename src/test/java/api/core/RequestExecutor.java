package api.core;

import api.spec.ResponseSpecs;
import ex.ApiException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.NonNull;

import java.util.Map;
import java.util.function.Function;

/**
 * Слой выполнения HTTP-запросов.
 *
 * <p><b>Зачем.</b> Клиенты ({@code *ApiClient}) получают набор default-методов,
 * покрывающих типовые HTTP-операции. В клиентах остаётся только то, что
 * действительно отличается вызов от вызова: путь, тело, тип ответа.
 * Всё остальное — построение запроса, валидация ответа, оборачивание
 * ошибок — сосредоточено здесь.
 *
 * <p><b>Уровни API.</b>
 * <ul>
 *     <li><b>Обёртки</b> {@code get/post/patch/delete} — 95% случаев.
 *         Дефолтная валидация — {@link ResponseSpecs#OK} (или
 *         {@link ResponseSpecs#NO_CONTENT} для {@code DELETE}).</li>
 *     <li><b>Перегрузки с {@link ResponseSpecification}</b> — когда
 *         ожидается не 200 (201, 400, 403, 404).</li>
 *     <li><b>Escape hatch</b> {@link #execute} / {@link #executeVoid} —
 *         для нестандартных случаев: multipart, streaming, кастомные
 *         заголовки, PUT и прочее. Принимают лямбду от
 *         {@link RequestSpecification} к {@link Response}.</li>
 * </ul>
 *
 * <p><b>Про {@link RequestSpecification}.</b> Каждый вызов ожидает <em>свежую</em>
 * спеку из {@link RestClient#request()} или {@link RestClient#authRequest(String)}.
 * Спека мутабельна и не должна переиспользоваться между вызовами.
 *
 * <p><b>Про {@link ResponseSpecification}.</b> Передаётся на каждый вызов:
 * одна и та же стратегия может быть нужна разным клиентам, а один клиент
 * может ожидать разные статусы в разных методах.
 *
 * @see RestClient
 * @see ResponseSpecs
 */
public interface RequestExecutor {

    /**
     * Выполняет произвольный запрос и десериализует тело ответа в {@code T}.
     *
     * <p>Использовать, когда обёрток {@code get/post/patch/delete} не хватает:
     * {@code PUT}, {@code OPTIONS}, multipart, кастомные заголовки и т.п.
     *
     * @param spec     спецификация запроса
     * @param respSpec стратегия валидации ответа
     * @param call     функция, выполняющая запрос; принимает спеку, возвращает {@link Response}
     * @param type     класс тела ответа
     * @param <T>      тип тела ответа
     * @return десериализованное тело
     * @throws ApiException если ответ не прошёл валидацию, тело пустое
     *                      или десериализация упала
     */
    default <T> T execute(@NonNull RequestSpecification spec,
                          @NonNull ResponseSpecification respSpec,
                          @NonNull Function<RequestSpecification, Response> call,
                          @NonNull Class<T> type) {
        Response response = call.apply(spec);
        response.then().spec(respSpec);
        T body = response.as(type);
        if (body == null) {
            throw new ApiException("Response body is null: " + response.asPrettyString());
        }
        return body;
    }

    /**
     * Вариант {@link #execute} для дженерик-ответов (например,
     * {@code RestResponsePage<ArtistJson>}), где нельзя использовать
     * {@code Class<T>}.
     *
     * @param spec     спецификация запроса
     * @param respSpec стратегия валидации ответа
     * @param call     функция, выполняющая запрос
     * @param type     {@link TypeRef} с полной параметризацией
     * @param <T>      тип тела ответа
     * @return десериализованное тело
     * @throws ApiException если ответ не прошёл валидацию, тело пустое
     *                      или десериализация упала
     */
    default <T> T execute(@NonNull RequestSpecification spec,
                          @NonNull ResponseSpecification respSpec,
                          @NonNull Function<RequestSpecification, Response> call,
                          @NonNull TypeRef<T> type) {
        Response response = call.apply(spec);
        response.then().spec(respSpec);
        T body = response.as(type);
        if (body == null) {
            throw new ApiException("Response body is null: " + response.asPrettyString());
        }
        return body;
    }

    /**
     * Вариант {@link #execute} без возвращаемого тела ответа.
     * Используется для {@code DELETE}, {@code PATCH} без ответа и т.п.
     *
     * @param spec     спецификация запроса
     * @param respSpec стратегия валидации ответа
     * @param call     функция, выполняющая запрос
     * @throws ApiException если ответ не прошёл валидацию
     */
    default void executeVoid(@NonNull RequestSpecification spec,
                             @NonNull ResponseSpecification respSpec,
                             @NonNull Function<RequestSpecification, Response> call) {
        call.apply(spec).then().spec(respSpec);
    }

    // ---------- GET ----------

    /**
     * {@code GET path}, ожидается 200, тело — {@code T}.
     *
     * @param spec спецификация запроса
     * @param path путь эндпоинта
     * @param type класс тела ответа
     * @param <T>  тип тела
     * @return десериализованное тело
     */
    default <T> T get(@NonNull RequestSpecification spec,
                      @NonNull String path,
                      @NonNull Class<T> type) {
        return execute(spec, ResponseSpecs.OK, s -> s.get(path), type);
    }

    /**
     * {@code GET path} с явной стратегией валидации.
     *
     * @param spec     спецификация запроса
     * @param path     путь эндпоинта
     * @param respSpec ожидаемая стратегия ответа
     * @param type     класс тела ответа
     * @param <T>      тип тела
     * @return десериализованное тело
     */
    default <T> T get(@NonNull RequestSpecification spec,
                      @NonNull String path,
                      @NonNull ResponseSpecification respSpec,
                      @NonNull Class<T> type) {
        return execute(spec, respSpec, s -> s.get(path), type);
    }

    /**
     * {@code GET path} с path-параметрами (шаблон вида {@code /artist/{id}}).
     * Ожидается 200, тело — {@code T}.
     *
     * @param spec       спецификация запроса
     * @param path       шаблон пути
     * @param pathParams значения path-параметров
     * @param type       класс тела ответа
     * @param <T>        тип тела
     * @return десериализованное тело
     */
    default <T> T get(@NonNull RequestSpecification spec,
                      @NonNull String path,
                      @NonNull Map<String, ?> pathParams,
                      @NonNull Class<T> type) {
        return execute(spec, ResponseSpecs.OK,
                s -> s.pathParams(pathParams).get(path), type);
    }

    /**
     * {@code GET path} с path- и query-параметрами, ответ — дженерик-тип
     * (например, {@code RestResponsePage<ArtistJson>}).
     *
     * <p>Пример:
     * <pre>{@code
     *   get(spec,
     *       "/internal/artist",
     *       Map.of(),
     *       Map.of("page", 0, "size", 20),
     *       new TypeRef<RestResponsePage<ArtistJson>>() {});
     * }</pre>
     *
     * @param spec        спецификация запроса
     * @param path        шаблон пути
     * @param pathParams  значения path-параметров (может быть пустой {@code Map})
     * @param queryParams значения query-параметров
     * @param type        {@link TypeRef} с полной параметризацией
     * @param <T>         тип тела
     * @return десериализованное тело
     */
    default <T> T get(@NonNull RequestSpecification spec,
                      @NonNull String path,
                      @NonNull Map<String, ?> pathParams,
                      @NonNull Map<String, ?> queryParams,
                      @NonNull TypeRef<T> type) {
        return execute(spec, ResponseSpecs.OK,
                s -> s.pathParams(pathParams).queryParams(queryParams).get(path), type);
    }


    /**
     * {@code POST path} с телом. Ожидается 200, тело — {@code T}.
     *
     * @param spec спецификация запроса
     * @param path путь эндпоинта
     * @param body тело запроса (сериализуется Jackson'ом)
     * @param type класс тела ответа
     * @param <T>  тип тела
     * @return десериализованное тело
     */
    default <T> T post(@NonNull RequestSpecification spec,
                       @NonNull String path,
                       @NonNull Object body,
                       @NonNull Class<T> type) {
        return execute(spec, ResponseSpecs.OK, s -> s.body(body).post(path), type);
    }

    /**
     * {@code POST path} с телом и явной стратегией (например, 201 Created).
     *
     * @param spec     спецификация запроса
     * @param path     путь эндпоинта
     * @param body     тело запроса
     * @param respSpec ожидаемая стратегия ответа
     * @param type     класс тела ответа
     * @param <T>      тип тела
     * @return десериализованное тело
     */
    default <T> T post(@NonNull RequestSpecification spec,
                       @NonNull String path,
                       @NonNull Object body,
                       @NonNull ResponseSpecification respSpec,
                       @NonNull Class<T> type) {
        return execute(spec, respSpec, s -> s.body(body).post(path), type);
    }


    /**
     * {@code PATCH path} с телом. Ожидается 200, тело — {@code T}.
     *
     * @param spec спецификация запроса
     * @param path путь эндпоинта
     * @param body тело запроса
     * @param type класс тела ответа
     * @param <T>  тип тела
     * @return десериализованное тело
     */
    default <T> T patch(@NonNull RequestSpecification spec,
                        @NonNull String path,
                        @NonNull Object body,
                        @NonNull Class<T> type) {
        return execute(spec, ResponseSpecs.OK, s -> s.body(body).patch(path), type);
    }

    /**
     * {@code PATCH path} с телом и явной стратегией валидации.
     *
     * @param spec     спецификация запроса
     * @param path     путь эндпоинта
     * @param body     тело запроса
     * @param respSpec ожидаемая стратегия ответа
     * @param type     класс тела ответа
     * @param <T>      тип тела
     * @return десериализованное тело
     */
    default <T> T patch(@NonNull RequestSpecification spec,
                        @NonNull String path,
                        @NonNull Object body,
                        @NonNull ResponseSpecification respSpec,
                        @NonNull Class<T> type) {
        return execute(spec, respSpec, s -> s.body(body).patch(path), type);
    }


    /**
     * {@code DELETE path}. Ожидается 204 No Content.
     *
     * @param spec спецификация запроса
     * @param path путь эндпоинта
     */
    default void delete(@NonNull RequestSpecification spec,
                        @NonNull String path) {
        executeVoid(spec, ResponseSpecs.NO_CONTENT, s -> s.delete(path));
    }

    /**
     * {@code DELETE path} с явной стратегией валидации
     * (если API возвращает 200 или иной код).
     *
     * @param spec     спецификация запроса
     * @param path     путь эндпоинта
     * @param respSpec ожидаемая стратегия ответа
     */
    default void delete(@NonNull RequestSpecification spec,
                        @NonNull String path,
                        @NonNull ResponseSpecification respSpec) {
        executeVoid(spec, respSpec, s -> s.delete(path));
    }
}