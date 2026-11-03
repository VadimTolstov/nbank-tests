package jupiter.extension;

import api.core.AuthContext;
import models.rest.AdminConstants;
import models.rest.CreateUserJsonResponse;
import models.rest.UserRole;
import org.junit.jupiter.api.extension.ExtensionContext;
import service.AdminClient;
import service.AuthClient;
import service.api.AdminApiClient;
import service.api.AuthApiClient;

public class CleanupSuiteExtension implements SuiteExtension {
    private final AdminClient adminClient = new AdminApiClient();
    private final AuthClient authClient = new AuthApiClient();

    @Override
    public void beforeSuite(ExtensionContext context) {
        authClient.authUser(AdminConstants.LOGIN, AdminConstants.PASSWORD);
        for (CreateUserJsonResponse user : adminClient.getUsers().stream()
                .filter(user -> !user.role().equals(UserRole.ADMIN))
                .toList()
        ) {
            adminClient.deleteUserById(user.id());
        }
        AuthContext.clear();    }
}
