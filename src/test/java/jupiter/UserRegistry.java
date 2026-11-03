package jupiter;

import models.rest.CreateUserJsonResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Реестр пользователей, созданных в рамках текущего теста.
 * Используется для последующей очистки.
 *
 * <p>Регистрируй всех созданных пользователей — и созданных
 * автоматически через {@code @User}/{@code @Data}, и созданных
 * вручную из тела теста.
 *
 * <p>{@link ThreadLocal} — изоляция параллельных тестов.
 */
public final class UserRegistry {

    private static final ThreadLocal<List<CreateUserJsonResponse>> CREATED =
            ThreadLocal.withInitial(ArrayList::new);

    private UserRegistry() {
    }

    /** Зарегистрировать созданного пользователя для удаления после теста. */
    public static void register(CreateUserJsonResponse user) {
        CREATED.get().add(user);
    }

    /** Все зарегистрированные пользователи текущего теста. */
    public static List<CreateUserJsonResponse> created() {
        return List.copyOf(CREATED.get());
    }

    public static void clear() {
        CREATED.remove();
    }
}
