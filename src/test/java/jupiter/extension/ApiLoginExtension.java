package jupiter.extension;

import api.core.AuthContext;
import jupiter.annotation.ApiLogin;
import jupiter.annotation.Data;
import jupiter.annotation.Token;
import models.TestData;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import service.AuthClient;
import service.api.AuthApiClient;

import java.util.Objects;

public class ApiLoginExtension implements BeforeEachCallback, ParameterResolver {

    private final AuthClient authClient = new AuthApiClient();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), ApiLogin.class)
                .ifPresent(apiLogin -> {

                    boolean empty = apiLogin.username().isEmpty() && apiLogin.password().isEmpty();
                    boolean full = !apiLogin.username().isEmpty() && !apiLogin.password().isEmpty();

                    if (!empty && !full) {
                        throw new IllegalStateException(
                                "В @ApiLogin укажи либо оба поля, либо ни одного.");
                    }

                    if (full) {
                        loginFixed(apiLogin);
                    } else {
                        loginDeclared(context);
                    }
                });
    }

    // ---------- режимы логина ----------

    /**
     * @ApiLogin(username=..., password=...) — фиксированный  пользователь.
     */
    private void loginFixed(ApiLogin apiLogin) {
        if (UserExtension.getUser() != null) {
            throw new IllegalStateException(
                    "@User нельзя сочетать с заполненным @ApiLogin(username=..., password=...).");
        }
        authClient.authUser(apiLogin.username(), apiLogin.password());
    }

    /**
     * @ApiLogin без параметров — логиним либо @User, либо всех из @Data.
     */
    private void loginDeclared(ExtensionContext ctx) {
        if (AnnotationSupport.isAnnotated(ctx.getRequiredTestMethod(), Data.class)) {
            TestData testData = TestDataExtension.getContent();
            if (testData == null || testData.usersJson().isEmpty()) {
                throw new IllegalStateException(
                        "Пустой @ApiLogin с @Data требует хотя бы одного пользователя.");
            }
            for (CreateUserJsonResponse user : testData.usersJson()) {
                authClient.authUser(user.username(), user.password());
            }
            return;
        }

        CreateUserJsonResponse user = UserExtension.getUser();
        if (user == null) {
            throw new IllegalStateException(
                    "Пустой @ApiLogin требует @User или @Data над методом.");
        }
        authClient.authUser(user.username(), user.password());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(String.class)
                && AnnotationSupport.isAnnotated(parameterContext.getParameter(), Token.class);
    }

    @Override
    public String resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        String username = resolveUsername(parameterContext, extensionContext);
        String token = AuthContext.get(username);
        if (token == null) {
            throw new IllegalStateException(
                    "Нет токена для пользователя '" + username + "'. Проверь @ApiLogin.");
        }
        return token;
    }

    /**
     * Приоритет:
     * <ol>
     *     <li>{@code @Token("username")} — явно указанный username.</li>
     *     <li>{@code @ApiLogin(username = ...)} — фиксированный пользователь.</li>
     *     <li>Единственный {@code @User}.</li>
     *     <li>Единственный пользователь из {@code @Data}.</li>
     * </ol>
     */
    private String resolveUsername(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // 1. @Token("username")
        String tokenUser = AnnotationSupport.findAnnotation(parameterContext.getParameter(), Token.class)
                .map(Token::username)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        if (tokenUser != null) {
            return tokenUser;
        }

        // 2. @ApiLogin(username = ...)
        String fixedUser = AnnotationSupport.findAnnotation(
                        extensionContext.getRequiredTestMethod(), ApiLogin.class)
                .map(ApiLogin::username)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        if (fixedUser != null) {
            return fixedUser;
        }

        // 3. Единственный @User
        CreateUserJsonResponse user = UserExtension.getUser();
        if (user != null) {
            return user.username();
        }

        // 4. Единственный из @Data
        if (AnnotationSupport.isAnnotated(extensionContext.getRequiredTestMethod(), Data.class)) {
            TestData data = TestDataExtension.getContent();
            if (data != null && data.usersJson().size() == 1) {
                return data.usersJson().stream().filter(Objects::nonNull).findFirst().get().username();
            }
            throw new IllegalStateException(
                    "При @Data с несколькими пользователями укажи @Token(\"username\"). "
                            + "Username-ы доступны через TestData.usersJson().");
        }
        throw new IllegalStateException(
                "Не удалось определить пользователя для @Token.");
    }

}
