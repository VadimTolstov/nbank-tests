package jupiter.extension;

import jupiter.annotation.Data;
import models.TestData;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonResponse;
import models.rest.UserRole;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import service.AdminClient;
import service.AuthClient;
import service.api.AdminApiClient;
import service.api.AuthApiClient;

import java.util.HashSet;

public class TestDataExtension implements BeforeEachCallback, AfterAllCallback, ParameterResolver {
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TestDataExtension.class);
    private final AdminClient adminClient = new AdminApiClient();
    private final AuthClient authClient = new AuthApiClient();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), Data.class)
                .ifPresent(annotation -> {
                    setContent(new TestData(new HashSet<>()));
                });
    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        authClient.authUser(AdminConstants.LOGIN, AdminConstants.PASSWORD);
        for (CreateUserJsonResponse user : adminClient.getUsers().stream()
                .filter(user -> !user.getRole().equals(UserRole.ADMIN))
                .toList()
        ) {
            adminClient.deleteUserById(user.getId());
        }
    }


    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getRequiredTestMethod().isAnnotationPresent(Data.class) &&
                parameterContext.getParameter().getType().equals(TestData.class);
    }

    @Override
    public TestData resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), TestData.class);
    }

    public static TestData getContent() {
        final ExtensionContext context = TestsMethodContextExtension.context();
        return context.getStore(NAMESPACE).get(context.getUniqueId(), TestData.class);
    }

    public static void setContent(TestData data) {
        final ExtensionContext context = TestsMethodContextExtension.context();
        context.getStore(NAMESPACE).put(context.getUniqueId(), data);
    }
}
