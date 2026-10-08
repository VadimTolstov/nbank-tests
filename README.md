# nbank-tests

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10.2-green.svg)](https://junit.org/junit5/)
[![REST Assured](https://img.shields.io/badge/REST%20Assured-6.0.0-blue.svg)](https://rest-assured.io/)
[![Allure](https://img.shields.io/badge/Allure-2.35.4-yellow.svg)](https://allurereport.org/)

Автотесты REST API банковского приложения **nbank**: пользователи, счета, депозиты, переводы, профиль.

Проект построен по принципам **Ports & Adapters (Hexagonal Architecture)** с расширяемой JUnit 5 инфраструктурой для декларативного описания тестовых данных.

---

## Содержание

- [Стек](#стек)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Быстрый старт](#быстрый-старт)
- [Как писать тесты](#как-писать-тесты)
    - [Базовый тест](#базовый-тест)
    - [Пользователь через `@User`](#пользователь-через-user)
    - [Несколько пользователей через `@Data`](#несколько-пользователей-через-data)
    - [Счета и балансы через `@Account`](#счета-и-балансы-через-account)
    - [Логин под фиксированным пользователем](#логин-под-фиксированным-пользователем)
    - [Токен в параметр теста](#токен-в-параметр-теста)
    - [Негативный тест](#негативный-тест)
- [Как добавить новый ресурс](#как-добавить-новый-ресурс)
- [Конфигурация](#конфигурация)
- [Известные ограничения](#известные-ограничения)
- [Roadmap](#roadmap)

---

## Стек

| Категория | Технология |
|---|---|
| Язык | Java 21 |
| Тестовый фреймворк | JUnit 5 (Jupiter 5.10.2) |
| HTTP-клиент | REST Assured 6.0.0 |
| Отчёты | Allure 2.35.4 |
| Сериализация | Jackson 2.22.1 |
| Утилиты | Lombok, Apache Commons Lang3 |
| Ассерты | AssertJ 3.27.7 |
| Генерация данных | RgxGen 1.3 |
| Логирование | SLF4J 2.0.17 (simple) |

---

## Архитектура

Проект разделён на слои по принципу **Ports & Adapters**:

```
┌────────────────────────────────────────────────────────────┐
│                           tests                            │
│         (JUnit 5 + кастомные аннотации @User, @Data)       │
└────────────────────────────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   service     │   │ jupiter.*     │   │   models      │
│   (порты)     │   │ (extensions)  │   │   (DTO)       │
└───────┬───────┘   └───────────────┘   └───────────────┘
        │
        ▼
┌────────────────────────────────────────────────────────────┐
│                      service.api                           │
│   AdminApiClient, AuthApiClient, AccountsApiClient, ...    │
└────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│                       api.core                             │
│    RestClient, RequestExecutor, AuthContext                │
│                      api.spec                              │
│                  ResponseSpecs                             │
│                    api.endpoint                            │
│           AdminEndpoints, AuthEndpoints, ...               │
└────────────────────────────────────────────────────────────┘
```

### Ключевые паттерны

| Паттерн | Где применяется | Зачем |
|---|---|---|
| **Ports & Adapters** | `service` (порт) / `service.api` (адаптер) | Транспорт отделён от домена: тесты не знают про RestAssured |
| **Facade** | `RestClient` | Инкапсулирует настройку RestAssured: baseUri, basePath, Jackson, фильтры, логирование |
| **Template Method** | `RequestExecutor` | Единая точка выполнения запросов; клиенты получают `get/post/patch/put/delete` бесплатно |
| **Strategy** | `ResponseSpecification` | Валидация статуса/тела передаётся параметром на каждый вызов |
| **Builder** | `RequestSpecBuilder`, `ResponseSpecBuilder` | Гибкая сборка спек без дублирования |
| **Chain of Responsibility** | RestAssured filters | Логирование, Allure, кастомные заголовки |
| **ThreadLocal-контекст** | `AuthContext`, `TestsMethodContextExtension` | Изоляция параллельных тестов |

### Многоуровневое хранение состояния

| Контекст | Что хранит | Где живёт | Кто чистит |
|---|---|---|---|
| `AuthContext` | `username → token` | `ThreadLocal<Map>` | `AuthExtension.afterEach` |
| `TestData` | Список созданных пользователей | `ExtensionContext.Store` (per-test) | JUnit сам |
| `UserRegistry` | Пользователи для удаления | `ThreadLocal<List>` | `UserExtension.afterEach` |
| `TestsMethodContextExtension` | `ExtensionContext` | `ThreadLocal` | `afterEach` |

---

## Структура проекта

```
src/test/java/
├── api/
│   ├── core/
│   │   ├── RestClient.java              # Фасад над RestAssured
│   │   ├── RequestExecutor.java         # Template Method: get/post/patch/put/delete
│   │   └── AuthContext.java             # ThreadLocal реестр токенов по username
│   ├── spec/
│   │   └── ResponseSpecs.java           # OK, CREATED, BAD_REQUEST, ... + фабрики
│   ├── endpoint/
│   │   ├── AccountsEndpoints.java
│   │   ├── AdminEndpoints.java
│   │   ├── AuthEndpoints.java
│   │   └── CustomerEndpoints.java
│   ├── ApiErrors.java                   # Константы сообщений об ошибках
│   └── ApiLimits.java                   # Границы (BigDecimal + String для аннотаций)
│
├── config/
│   ├── Config.java                      # Интерфейс конфига
│   ├── LocalConfig.java                 # http://127.0.0.1:4111
│   └── DockerConfig.java                # Для docker-окружения
│
├── ex/
│   └── ApiException.java                # Runtime-обёртка транспортных ошибок
│
├── generators/
│   ├── RandomData.java                  # Генерация username / password / fake token
│   ├── RandomModelGenerator.java        # Генерация моделей через рефлексию (record + POJO)
│   └── GeneratingRule.java              # @GeneratingRule(regex = "...")
│
├── jupiter/
│   ├── annotation/
│   │   ├── User.java                    # @User — создать пользователя
│   │   ├── Data.java                    # @Data — несколько пользователей
│   │   ├── Account.java                 # @Account — счёт с балансом
│   │   ├── ApiLogin.java                # @ApiLogin — залогинить
│   │   ├── AdminApiLogin.java           # @AdminApiLogin — залогинить под админом
│   │   ├── Token.java                   # @Token — инжект токена в параметр
│   │   └── meta/RestTest.java           # @RestTest — мета-аннотация для REST-тестов
│   ├── extension/
│   │   ├── TestsMethodContextExtension.java   # ThreadLocal к ExtensionContext
│   │   ├── TestDataExtension.java             # Создаёт TestData при @Data
│   │   ├── UserExtension.java                 # @User / @Data → пользователи
│   │   ├── ApiLoginExtension.java             # @ApiLogin / @Token
│   │   ├── AccountExtension.java              # @Account → счета и депозиты
│   │   ├── AuthExtension.java                 # Чистит AuthContext в afterEach
│   │   ├── CleanupSuiteExtension.java         # Suite-level очистка
│   │   └── SuiteExtension.java                # Базовый интерфейс для suite-хуков
│   └── UserRegistry.java                # ThreadLocal реестр созданных юзеров
│
├── models/
│   ├── TestData.java                    # Контейнер пользователей + requireByUsername
│   ├── comparison/
│   │   ├── ModelAssertions.java         # assertThatModels(...).match()
│   │   ├── ModelComparator.java         # Сравнение по маппингу (учёт BigDecimal.scale)
│   │   └── ModelComparisonConfigLoader.java   # Парсит model-comparison.properties
│   └── rest/
│       ├── UserRole.java
│       ├── CreateUserJsonRequest.java
│       ├── CreateUserJsonResponse.java  # + @JsonIgnore метаданные: randomAccounts, accountMetas
│       ├── CustomerAccountJson.java
│       ├── CustomerProfileJsonResponse.java
│       ├── DepositJsonRequest.java
│       ├── DepositJsonResponse.java
│       ├── TransferJson.java
│       ├── UpdateUserNameRequest.java
│       ├── UpdateUserNameResponse.java
│       ├── AdminConstants.java
│       └── AdminCredentials.java
│
├── service/
│   ├── AdminClient.java                 # ПОРТ
│   ├── AuthClient.java                  # ПОРТ
│   ├── AccountsClient.java              # ПОРТ
│   ├── CustomerClient.java              # ПОРТ
│   └── api/                             # АДАПТЕРЫ
│       ├── AdminApiClient.java          # + createUsersExpectingError (REST-specific)
│       ├── AuthApiClient.java
│       ├── AccountsApiClient.java       # + performDeposit/TransferExpectingError
│       └── CustomerApiClient.java       # + updateNameExpectingError
│
├── tests/
│   ├── CreateUserTest.java
│   ├── CreateAccountTest.java
│   ├── DepositTest.java
│   ├── LoginUserTest.java
│   ├── TransferTest.java
│   └── UpdateUserNameTest.java
│
└── utils/
    └── Repeat.java                      # N повторений Runnable

src/test/resources/
├── META-INF/
│   └── services/
│       └── org.junit.jupiter.api.extension.Extension             # AuthExtension и др.
├── config.properties
├── http-request.ftl                     # Allure-шаблон запроса
├── http-response.ftl                    # Allure-шаблон ответа
├── jndi.properties
├── junit-platform.properties            # Параллельный прогон + autodetection
└── model-comparison.properties          # Правила сравнения DTO
```

---

## Быстрый старт

### Требования

- **JDK 21+**
- **Maven 3.9+**
- Запущенный сервис **nbank** на `http://127.0.0.1:4111`

### Запуск

```bash
# Весь набор тестов
mvn clean test

# Один тест-класс
mvn test -Dtest=DepositTest

# Один тест
mvn test -Dtest=CreateUserTest#adminCanCreateUserWithCorrectData

# Параллельный прогон (уже включён в junit-platform.properties)
mvn test
```

### Allure-отчёт

```bash
mvn allure:serve     # открыть отчёт в браузере
mvn allure:report    # сгенерировать в target/site/allure-maven-plugin
```

---

## Как писать тесты

Все REST-тесты помечаются **мета-аннотацией `@RestTest`** — она подключает весь стек extensions.

### Базовый тест

```java
@RestTest
class CreateAccountTest {

    private final AccountsClient accountsClient = new AccountsApiClient();
    private final CustomerClient customerClient = new CustomerApiClient();

    @User
    @ApiLogin
    @Test
    void userCanCreateAccountTest(CreateUserJsonResponse user) {
        CustomerAccountJson account = accountsClient.createAccount(
                new CreateUserJsonRequest(user.username(), user.password(), user.role()));

        CustomerAccountJson byId = customerClient.getAccountById(
                user.username(), account.id());

        ModelAssertions.assertThatModels(account, byId).match();
    }
}
```

### Пользователь через `@User`

```java
@User                                     // случайный username/password
@ApiLogin
@Test
void test(CreateUserJsonResponse user) { /* user уже создан и залогинен */ }

@User(username = "alice", password = "pass123$")   // фиксированные креды
@ApiLogin
@Test
void test(CreateUserJsonResponse alice) { /* ... */ }
```

### Несколько пользователей через `@Data`

```java
@Data(
    users = {
        @User(username = "sender",   accounts = @Account(balance = "5000")),
        @User(username = "receiver")
    },
    randomUsers = 2                       // + 2 случайных пользователя
)
@ApiLogin
@Test
void transferBetweenUsersTest(TestData data) {
    CreateUserJsonResponse sender   = data.requireByUsername("sender");
    CreateUserJsonResponse receiver = data.requireByUsername("receiver");
    // ... + 2 случайных доступны через data.usersJson()
}
```

**Что делает `@Data`:** создаёт пользователей, регистрирует их в `TestData` и логинит всех под `@ApiLogin`.

### Счета и балансы через `@Account`

```java
@Data(users = {
    @User(username = "rich", accounts = {
        @Account(balance = "10000"),
        @Account(balance = "0.01")
    }),
    @User(username = "poor", randomAccounts = 1)   // счёт с нулевым балансом
})
@ApiLogin
@Test
void test(TestData data) {
    CreateUserJsonResponse rich = data.requireByUsername("rich");
    // rich.accounts() — список из 2 счетов: 10000 и 0.01
    // poor.accounts() — 1 счёт с балансом 0
}
```

`@Account(balance = "...")` — **строка**, т.к. `BigDecimal` не выражается в аннотации.
Баланс начисляется через `AccountExtension` (вызовы `POST /accounts` + `POST /accounts/deposit`).

### Логин под фиксированным пользователем

```java
@ApiLogin(username = "admin", password = "admin")
@Test
void adminCanDoSomething(@Token String adminToken) { /* ... */ }

@AdminApiLogin                  // == @ApiLogin(username = "admin", password = "admin")
@Test
void adminCanCreateUserTest() { /* ... */ }
```

### Токен в параметр теста

```java
@Data(users = {
    @User(username = "alice"),
    @User(username = "bob")
})
@ApiLogin
@Test
void multiUserTest(@Token("alice") String aliceToken,
                   @Token("bob")   String bobToken) {
    // aliceToken и bobToken — готовые значения из AuthContext
}
```

### Негативный тест

Негативные сценарии идут **через адаптер**, не через порт — методы `*ExpectingError` объявлены прямо в `*ApiClient`:

```java
@AdminApiLogin
@ParameterizedTest
@MethodSource("userInvalidData")
void adminCanNotCreateUserWithInvalidData(String username, String password,
                                          UserRole role, String errorKey, String errorValue) {
    adminClient.createUsersExpectingError(
            new CreateUserJsonRequest(username, password, role),
            errorKey, errorValue);
}
```

`createUsersExpectingError` внутри использует `executeVoid` + `ResponseSpecs.errorWithField(400, ...)`.

---

## Как добавить новый ресурс

1. **Endpoint** — `api/endpoint/FooEndpoints.java`:

   ```java
   public interface FooEndpoints {
       String FOO = "foo";           // без ведущего "/"
       String FOO_BY_ID = FOO + "/{id}";
   }
   ```

2. **Порт** — `service/FooClient.java`:

   ```java
   public interface FooClient {
       FooJson getFoo(UUID id);
       FooJson createFoo(CreateFooJsonRequest request);
   }
   ```

3. **Адаптер** — `service/api/FooApiClient.java`:

   ```java
   public class FooApiClient implements FooClient, RequestExecutor {
       private final RestClient restClient = new RestClient.EmptyRestClient(
               CFG.nbankUrl(), CFG.apiBasePathV1(), false, LogDetail.ALL);

       @Override
       public FooJson getFoo(UUID id) {
           return get(restClient.authRequest("admin"),
                   FooEndpoints.FOO_BY_ID,
                   Map.of("id", id),
                   FooJson.class);
       }
   }
   ```

4. **DTO** — `models/rest/FooJson.java` (record + Jackson).

5. **Тест** — использует порт и `@RestTest`.

---

## Конфигурация

### `junit-platform.properties`

```properties
junit.jupiter.extensions.autodetection.enabled=true       # читать META-INF/services
junit.jupiter.execution.parallel.enabled=true             # параллельный прогон
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=3
junit.jupiter.execution.parallel.config.fixed.max-pool-size=3
```

### `model-comparison.properties`

Формат: `RequestClass=ResponseClass:field1=responseField1,field2=responseField2`

```properties
CreateUserJsonRequest=CreateUserResponse:username=username,role=role
CustomerAccountJson=CustomerAccountJson:id=id,accountNumber=accountNumber,balance=balance
```

Сравнение идёт через `ModelAssertions.assertThatModels(req, resp).match()`.
Для `BigDecimal` используется `compareTo`, поэтому `500` и `500.00` считаются равными.

### `Config`

Определяет адрес сервиса:

```java
LocalConfig.instance.nbankUrl();   // http://127.0.0.1:4111/
```

Переключение: `-Dtest.env=docker`.

### Логирование

Настроено в `RestClient`:
- **запрос** — всегда, уровень `LogDetail.ALL` (или `HEADERS`);
- **ответ** — только если валидация упала;
- **Allure** — всегда, через `AllureRestAssured` с кастомными шаблонами `http-request.ftl` / `http-response.ftl`.

---

## Известные ограничения

- **`AuthContext` — ThreadLocal.** Обязательно чистится в `afterEach` (`AuthExtension`). Без `autodetection.enabled=true` extension не подхватится, и токены протекут между тестами.
- **Параллельный прогон.** `parallelism=3` + `mode.default=concurrent`. Тесты, использующие общие данные (`admin`, фиксированные username), могут конфликтовать. Для изоляции использовать случайные username (`RandomData.getUsername()`) или `@Data` с явными именами.
- **`@Account(balance = "...")` — только строки.** Числовые литералы ограничены диапазоном `double`, поэтому `BigDecimal` через строку.
- **`CleanupSuiteExtension`** зависит от корректного запуска `LauncherSession`. В IntelliJ IDE работает через `LauncherSessionListener`, в Maven — через `SuiteExtension`. Если пользователи остаются после прогона — проверь `META-INF/services/org.junit.platform.launcher.LauncherSessionListener`.
- **`UpdateUserNameTest`** использует **сырой JSON** (`updateNameExpectingError(username, rawJson, ...)`) для проверки кейсов невалидного JSON, например `{"name":John}` (без кавычек).
- **`UserExtension`** удаляет только тех пользователей, что создал сам (через `UserRegistry`). Юзеры, созданные из теста напрямую через `adminClient.createUsers(...)`, не удаляются автоматически — их нужно регистрировать вручную: `UserRegistry.register(created)`.
- **`CleanupSuiteExtension`** удаляет пользователей созданных в ручную перед запуском тестов, а не после всех тестов, из-за --> IntelliJ использует свой JUnitStarter, который не проходит через полный Launcher lifecycle. Root ExtensionContext создаётся, beforeAll срабатывает, но его закрытие (а с ним и AutoCloseable.close()) IntelliJ-runner не инициирует. Известная особенность.

---

## Roadmap

- [ ] Перевести `TestData` на `Set` + `equals` по `id` (или оставить `List`)
- [ ] `AdminDbClient` — адаптер через JDBC (для setup без API)
- [ ] Параметризация `@User(role = ADMIN)` (сейчас всегда `USER`)
- [ ] Матрица `@Tag("smoke")` / `@Tag("regression")` для выборочного запуска
- [ ]  Allure-отчёт

---