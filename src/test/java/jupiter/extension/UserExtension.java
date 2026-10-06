package jupiter.extension;

import generators.RandomData;
import jupiter.annotation.Account;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonResponse;
import models.rest.UserJson;
import models.rest.UserRole;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import service.AdminClient;
import service.AuthClient;
import service.api.AdminApiClient;
import service.api.AuthApiClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserExtension implements BeforeEachCallback, ParameterResolver {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

    private final AuthClient authClient = new AuthApiClient();
    private final AdminClient adminClient = new AdminApiClient();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Data dataAnno = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), Data.class).orElse(null);
        User userAnno = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), User.class).orElse(null);

        if (dataAnno == null && userAnno == null) {
            return;
        }

        if (dataAnno != null && userAnno != null) {
            throw new IllegalStateException(
                    "Нельзя использовать @Data и @User одновременно на методе "
                            + context.getRequiredTestMethod().getName());
        }

        authClient.authUser(AdminConstants.LOGIN,
                AdminConstants.PASSWORD);

        if (dataAnno != null) {
            TestDataExtension.getContent().usersJson()
                    .addAll(createFromData(dataAnno));
        } else {
            setUser(createWithMetadata(buildUser(userAnno), userAnno));

        }
    }

    private List<CreateUserJsonResponse> createFromData(Data dataAnno) {
        List<CreateUserJsonResponse> created = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(dataAnno.users())) {
            for (User userAnno : dataAnno.users()) {
                created.add(createWithMetadata(buildUser(userAnno), userAnno));
            }
        }
        for (int i = 0; i < dataAnno.randomUsers(); i++) {
            created.add(createRandom());
        }
        return created;
    }

    /** Пользователь, созданный из @User-аннотации — с метаданными счетов. */
    private CreateUserJsonResponse createWithMetadata(UserJson draft, User userAnno) {
        return adminClient.createUsers(draft)
                .toBuilder()
                .password(draft.password())
                .randomAccounts(userAnno.randomAccounts())
                .accountMetas(toMetas(userAnno.accounts()))
                .build();
    }

    /** Случайный пользователь — без счёт-метаданных. */
    private CreateUserJsonResponse createRandom() {
        UserJson draft = buildRandomUser();
        return adminClient.createUsers(draft)
                .toBuilder()
                .password(draft.password())
                .randomAccounts(0)
                .accountMetas(new ArrayList<>())
                .build();
    }

    private List<CreateUserJsonResponse.AccountMeta> toMetas(Account[] accounts) {
        if (ArrayUtils.isEmpty(accounts)) {
            return List.of();
        }
        return Arrays.stream(accounts).map(a -> new CreateUserJsonResponse.AccountMeta(a.balance()))
                .toList();
    }

    private UserJson buildUser(User userAnno) {
        return new UserJson(
                "".equals(userAnno.username()) ? RandomData.getUsername() : userAnno.username(),
                "".equals(userAnno.password()) ? RandomData.getPassword() : userAnno.password(),
                UserRole.USER
        );
    }

    private UserJson buildRandomUser() {
        return new UserJson(
                RandomData.getUsername(),
                RandomData.getPassword(),
                UserRole.USER
        );
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getRequiredTestMethod().isAnnotationPresent(User.class) &&
                parameterContext.getParameter().getType().equals(CreateUserJsonResponse.class);
    }

    @Override
    public CreateUserJsonResponse resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getUser();
    }

    public static CreateUserJsonResponse getUser() {
        final ExtensionContext context = TestsMethodContextExtension.context();
        return context.getStore(NAMESPACE).get(context.getUniqueId(), CreateUserJsonResponse.class);
    }

    public static void setUser(CreateUserJsonResponse user) {
        final ExtensionContext context = TestsMethodContextExtension.context();
        context.getStore(NAMESPACE).put(
                context.getUniqueId(),
                user
        );
    }
}
