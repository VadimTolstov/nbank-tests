package jupiter.extension;

import api.ApiLimits;
import jupiter.annotation.Data;
import jupiter.annotation.User;
import models.rest.CreateUserJsonResponse;
import models.rest.CustomerAccountJson;
import models.rest.DepositJsonRequest;
import models.rest.CreateUserJsonRequest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import service.AccountsClient;
import service.api.AccountsApiClient;

import java.math.BigDecimal;

public class AccountExtension implements BeforeEachCallback {
    private final AccountsClient accountsClient = new AccountsApiClient();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Data dataAnno = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), Data.class).orElse(null);
        User userAnno = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), User.class).orElse(null);

        if (dataAnno == null && userAnno == null) {
            return;
        }

        if (dataAnno != null) {
            TestDataExtension.getContent().usersJson()
                    .forEach(this::processAccounts);
        }

        if (userAnno != null) {
            processAccounts(UserExtension.getUser());
        }
    }

    private void processAccounts(CreateUserJsonResponse userResponse) {
        CreateUserJsonRequest createUserJsonRequest = new CreateUserJsonRequest(
                userResponse.username(),
                userResponse.password(),
                userResponse.role()
        );

        for (int i = 0; i < userResponse.randomAccounts(); i++) {
            userResponse.accounts().add(accountsClient.createAccount(createUserJsonRequest));
        }

        for (CreateUserJsonResponse.AccountMeta meta : userResponse.accountMetas()) {
            CustomerAccountJson account = accountsClient.createAccount(createUserJsonRequest);
            userResponse.accounts().add(account);

            BigDecimal total = new BigDecimal(meta.balance());
            if (total.signum() > 0) {
                BigDecimal left = total;
                while (left.signum() > 0) {
                    BigDecimal part = left.min(ApiLimits.DEPOSIT_MAX);
                    accountsClient.deposit(createUserJsonRequest.username(), new DepositJsonRequest(account.id(), part));
                    left = left.subtract(part);
                }
            }

        }
    }
}
