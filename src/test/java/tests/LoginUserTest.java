package tests;

import generators.RandomModelGenerator;
import jupiter.annotation.AdminApiLogin;
import jupiter.annotation.meta.RestTest;
import models.rest.AdminConstants;
import models.rest.UserJson;
import models.rest.UserRole;
import org.junit.jupiter.api.Test;
import service.AdminClient;
import service.AuthClient;
import service.api.AdminApiClient;
import service.api.AuthApiClient;

@RestTest
public class LoginUserTest {
    private final AdminClient adminClient = new AdminApiClient();
    private final AuthClient authClient = new AuthApiClient();

    @Test
    public void adminCanGenerateAuthTokenTest() {
        UserJson user = new UserJson(AdminConstants.LOGIN, AdminConstants.PASSWORD, UserRole.ADMIN);
        authClient.authUser(user.username(), user.password());
    }

    @AdminApiLogin
    @Test
    public void userCanGenerateAuthTokenTest() {
        UserJson user = RandomModelGenerator.generate(UserJson.class);
        adminClient.createUsers(user);

        authClient.authUser(user.username(), user.password());
    }
}