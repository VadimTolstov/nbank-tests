package api.core;

import config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.RequestSpecification;
import lombok.NonNull;

/**
 * Фасад над RestAssured. Инкапсулирует настройку транспорта для группы
 * эндпоинтов с общим {@code baseUri} и опциональным {@code basePath}.
 *
 * <p><b>Что здесь настраивается:</b>
 * <ul>
 *     <li>базовый URL, базовый path API, {@code Content-Type}, {@code Accept};</li>
 *     <li>Jackson как ObjectMapper (JACKSON_2);</li>
 *     <li>таймауты соединения и сокета;</li>
 *     <li>следование редиректам;</li>
 *     <li>уровень логирования запроса;</li>
 *     <li>логирование запроса и ответа при падении валидации;</li>
 *     <li>Allure-фильтр для отчёта;</li>
 *     <li>произвольные дополнительные {@link Filter}.</li>
 * </ul>
 *
 * <p><b>Зачем разделять {@code baseUri} и {@code basePath}.</b>
 * {@code baseUri} — это адрес сервиса (схема, хост, порт), например
 * {@code http://127.0.0.1:4111}. {@code basePath} — префикс API, например
 * {@code /api/v1} или {@code /internal}. RestAssured склеивает их при
 * выполнении запроса: {@code baseUri + basePath + path}. Если вклеить
 * префикс в {@code baseUri}, RestAssured теряет понятие {@code basePath},
 * а путь с ведущим {@code /} в эндпоинте перезапишет префикс целиком.
 *
 * <p><b>Важно про пути в {@code *Endpoints}.</b> Пути ресурсов указывай
 * <em>без</em> ведущего {@code /}. Иначе RestAssured посчитает путь
 * абсолютным и проигнорирует {@code basePath}:
 * <pre>{@code
 *   // правильно
 *   String AUTH_USER = "auth/login";
 *
 *   // неправильно — basePath будет проигнорирован
 *   String AUTH_USER = "/auth/login";
 * }</pre>
 *
 * <p><b>Логирование.</b> Устроено в три слоя:
 * <ul>
 *     <li><b>запрос всегда</b> — на уровне {@code logDetail} через
 *         {@code RequestSpecBuilder#log(LogDetail)};</li>
 *     <li><b>ответ только при падении валидации</b> — через
 *         {@link LogConfig#enableLoggingOfRequestAndResponseIfValidationFails(LogDetail)};</li>
 *     <li><b>в Allure всегда</b> — через {@link AllureRestAssured}.</li>
 * </ul>
 * За счёт этого зелёные тесты не засоряют консоль, а упавшие сразу
 * показывают тело ответа.
 *
 * <p><b>Авторизация.</b> См. {@link #authRequest(String)} — токен берётся
 * из {@link AuthContext}, что позволяет в одном тесте держать несколько
 * клиентов под разными пользователями.
 *
 * <p><b>Использование.</b>
 * <pre>{@code
 *   // nbank API
 *   RestClient nbank = new RestClient.EmptyRestClient(
 *           CFG.nbankUrl(),          // http://127.0.0.1:4111
 *           CFG.apiBasePathV1(),     // /api/v1
 *           false,
 *           LogDetail.ALL);
 *
 *   RequestSpecification spec = nbank.request();
 *
 *   // rococo artist API — свой префикс
 *   RestClient artist = new RestClient.EmptyRestClient(
 *           CFG.artistUrl(),
 *           "/internal",
 *           false,
 *           LogDetail.HEADERS);
 * }</pre>
 *
 * @see RequestExecutor
 * @see api.spec.ResponseSpecs
 */
public abstract class RestClient {

    /**
     * Единая точка доступа к конфигу проекта. Доступна наследникам.
     */
    protected static final Config CFG = Config.getInstance();

    /**
     * Базовая спецификация запроса. Immutable, переиспользуется:
     * каждый вызов {@link #request()} / {@link #authRequest(String)}
     * делает свежую копию из неё.
     */
    protected final RequestSpecification baseSpec;

    /**
     * Создаёт клиент с дефолтными настройками: без {@code basePath},
     * без редиректов, логирование запроса на уровне
     * {@link LogDetail#HEADERS}.
     *
     * @param baseUrl базовый URL API (схема + хост + порт, без trailing slash)
     */
    protected RestClient(String baseUrl) {
        this(baseUrl, null, false, LogDetail.HEADERS);
    }

    /**
     * Создаёт клиент с настройкой следования редиректам.
     *
     * @param baseUrl        базовый URL API
     * @param followRedirect следовать ли HTTP-редиректам
     */
    protected RestClient(String baseUrl, boolean followRedirect) {
        this(baseUrl, null, followRedirect, LogDetail.HEADERS);
    }

    /**
     * Создаёт клиент с {@code basePath} и настройкой уровня логирования.
     *
     * @param baseUrl        базовый URL API
     * @param basePath       префикс API (например, {@code /api/v1});
     *                       может быть {@code null}, если префикса нет
     * @param followRedirect следовать ли HTTP-редиректам
     * @param logDetail      уровень логирования запроса
     */
    protected RestClient(String baseUrl,
                         String basePath,
                         boolean followRedirect,
                         LogDetail logDetail) {
        this(baseUrl, basePath, followRedirect, logDetail, new Filter[0]);
    }

    /**
     * Полный конструктор.
     *
     * @param baseUrl        базовый URL API (схема + хост + порт, без trailing slash)
     * @param basePath       префикс API (например, {@code /api/v1}); может быть {@code null}
     * @param followRedirect следовать ли HTTP-редиректам
     * @param logDetail      уровень логирования запроса; при падении валидации
     *                       на этом же уровне логируется ответ
     * @param extraFilters   дополнительные фильтры RestAssured
     *                       (например, кастомные заголовки, cookies)
     */
    protected RestClient(String baseUrl,
                         String basePath,
                         boolean followRedirect,
                         LogDetail logDetail,
                         Filter... extraFilters) {

        RestAssuredConfig config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 10_000)
                        .setParam("http.socket.timeout", 30_000)
                        .setParam("http.connection-manager.timeout", 30_000L)
                        .setParam("http.protocol.handle-redirects", followRedirect))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .defaultObjectMapperType(ObjectMapperType.JACKSON_2))
                // логировать запрос И ответ только при падении валидации
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(logDetail));

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setConfig(config)
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                // постоянное логирование запроса на заданном уровне
                .log(logDetail)
                .addFilter(new AllureRestAssured()
                        .setRequestTemplate("http-request.ftl")
                        .setResponseTemplate("http-response.ftl"));

        if (basePath != null && !basePath.isBlank()) {
            builder.setBasePath(basePath);
        }

        for (Filter f : extraFilters) {
            builder.addFilter(f);
        }

        this.baseSpec = builder.build();
    }

    /**
     * Возвращает свежую спецификацию запроса без авторизации.
     *
     * <p>Возвращаемый объект мутабельный — его нужно получать заново
     * на каждый запрос и не шарить между вызовами. Внутренний
     * {@link #baseSpec} при этом не меняется: {@code given().spec(...)}
     * создаёт копию.
     *
     * @return новая {@link RequestSpecification}
     */
    @NonNull
    public RequestSpecification request() {
        return RestAssured.given().spec(baseSpec);
    }

    /**
     * Возвращает свежую спецификацию запроса с заголовком
     * {@code Authorization: <token>} под указанным пользователем.
     *
     * <p>Токен берётся из {@link AuthContext}. Если токен не задан —
     * бросается {@link IllegalStateException}: молчаливый уход в
     * неавторизованный запрос опаснее явной ошибки конфигурации теста.
     *
     * <p>Обрати внимание: заголовок подставляется <em>как есть</em>,
     * без префикса {@code Bearer}. Если твой API требует
     * {@code Authorization: Bearer <token>}, сохраняй в
     * {@link AuthContext} уже полное значение — с префиксом.
     *
     * @param username имя пользователя, для которого зарегистрирован токен
     * @return новая {@link RequestSpecification} с заголовком авторизации
     * @throws IllegalStateException если токен для пользователя не задан
     */
    @NonNull
    public RequestSpecification authRequest(@NonNull String username) {
        String token = AuthContext.get(username);
        if (token == null) {
            throw new IllegalStateException(
                    "Нет токена для пользователя '" + username);
        }
        return RestAssured.given().spec(baseSpec)
                .header("Authorization", token);
    }

    /**
     * «Пустой» клиент с заданными {@code baseUrl} и опциональным
     * {@code basePath}, без доменной специфики.
     *
     * <p>Удобен, когда нужен просто транспорт для одного ресурса:
     * <pre>{@code
     *   // nbank
     *   RestClient nbank = new RestClient.EmptyRestClient(
     *           CFG.nbankUrl(), CFG.apiBasePathV1(), false, LogDetail.ALL);
     *
     *   // rococo artist
     *   RestClient artist = new RestClient.EmptyRestClient(
     *           CFG.artistUrl(), "/internal");
     * }</pre>
     */
    public static class EmptyRestClient extends RestClient {

        /**
         * @param baseUrl базовый URL API
         */
        public EmptyRestClient(@NonNull String baseUrl) {
            super(baseUrl);
        }

        /**
         * @param baseUrl  базовый URL API
         * @param basePath префикс API (например, {@code /api/v1});
         *                 может быть {@code null}
         */
        public EmptyRestClient(@NonNull String baseUrl, String basePath) {
            super(baseUrl, basePath, false, LogDetail.HEADERS);
        }

        /**
         * @param baseUrl        базовый URL API
         * @param basePath       префикс API
         * @param followRedirect следовать ли редиректам
         */
        public EmptyRestClient(@NonNull String baseUrl,
                               String basePath,
                               boolean followRedirect) {
            super(baseUrl, basePath, followRedirect, LogDetail.HEADERS);
        }

        /**
         * @param baseUrl        базовый URL API
         * @param basePath       префикс API
         * @param followRedirect следовать ли редиректам
         * @param logDetail      уровень логирования запроса
         */
        public EmptyRestClient(@NonNull String baseUrl,
                               String basePath,
                               boolean followRedirect,
                               @NonNull LogDetail logDetail) {
            super(baseUrl, basePath, followRedirect, logDetail);
        }

        /**
         * @param baseUrl        базовый URL API
         * @param basePath       префикс API
         * @param followRedirect следовать ли редиректам
         * @param logDetail      уровень логирования запроса
         * @param filters        дополнительные фильтры RestAssured
         */
        public EmptyRestClient(@NonNull String baseUrl,
                               String basePath,
                               boolean followRedirect,
                               @NonNull LogDetail logDetail,
                               @NonNull Filter... filters) {
            super(baseUrl, basePath, followRedirect, logDetail, filters);
        }
    }
}