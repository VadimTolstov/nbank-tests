package api.core;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр токенов авторизации в рамках текущего потока.
 *
 * <p>Хранит пары {@code username -> token} в {@link ThreadLocal}. Это даёт
 * два свойства:
 * <ul>
 *     <li><b>multi-user в одном тесте</b> — можно держать токены нескольких
 *         пользователей одновременно и переключаться между ними, конструируя
 *         клиентов под нужного пользователя;</li>
 *     <li><b>изоляция параллельных прогонов</b> — при
 *         {@code junit.jupiter.execution.parallel.enabled=true} каждый поток
 *         видит только свои токены.</li>
 * </ul>
 *
 * <p><b>Жизненный цикл.</b> Обязательно вызывать {@link #clear()} в
 * {@code @AfterEach}. Иначе при переиспользовании потока JUnit токены
 * одного теста протекут в следующий.
 *
 * <p><b>Пример использования:</b>
 * <pre>{@code
 *   @BeforeEach
 *   void setUp() {
 *       AuthContext.put("admin", adminToken);
 *       AuthContext.put("user1", user1Token);
 *   }
 *
 *   @AfterEach
 *   void tearDown() {
 *       AuthContext.clear();
 *   }
 * }</pre>
 *
 * @see api.core.RestClient#authRequest(String)
 */
public final class AuthContext {

    /**
     * Карта {@code username -> token} для текущего потока.
     */
    private static final ThreadLocal<Map<String, String>> TOKENS =
            ThreadLocal.withInitial(HashMap::new);

    private AuthContext() {
        // utility-класс, инстанцирование запрещено
    }

    /**
     * Регистрирует токен для пользователя в текущем потоке.
     * Повторный вызов с тем же {@code username} перезаписывает значение.
     *
     * @param username имя пользователя (не {@code null})
     * @param token    токен авторизации (не {@code null})
     */
    public static void put(@NonNull String username, @NonNull String token) {
        TOKENS.get().put(username, token);
    }

    /**
     * Возвращает токен пользователя или {@code null}, если токен не задан.
     *
     * @param username имя пользователя (не {@code null})
     * @return токен или {@code null}
     */
    public static @Nullable String get(@NonNull String username) {
        return TOKENS.get().get(username);
    }

    /**
     * Проверяет, зарегистрирован ли токен для пользователя.
     *
     * @param username имя пользователя (не {@code null})
     * @return {@code true}, если токен есть
     */
    public static boolean has(@NonNull String username) {
        return TOKENS.get().containsKey(username);
    }

    /**
     * Очищает все токены текущего потока. Вызывать в {@code @AfterEach}.
     */
    public static void clear() {
        TOKENS.remove();
    }
}