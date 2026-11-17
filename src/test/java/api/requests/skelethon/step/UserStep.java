package api.requests.skelethon.step;

import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.GetAccountsRequester;
import api.requests.skelethon.requesters.ValidatedProfileRequester;
import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import lombok.NonNull;
import models.rest.CustomerAccountJson;
import models.rest.CustomerProfileJsonResponse;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UserStep {

    public static @Nullable List<CustomerAccountJson> getAllAccounts(@NonNull String username) {
        return new GetAccountsRequester(
                RequestSpecs.authAsUser(username),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.OK)
                .getEndpoint();
    }

    public static @Nullable CustomerAccountJson getAccountById(@NonNull String username, long accountId) {
        return new GetAccountsRequester(
                RequestSpecs.authAsUser(username),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.OK)
                .getEndpoint()
                .stream()
                .filter(account -> account.id() == accountId)
                .findFirst()
                .orElse(null);
    }

    public static CustomerProfileJsonResponse getProfile(@NonNull String username) {
        return (CustomerProfileJsonResponse) new ValidatedProfileRequester<>(
                RequestSpecs.authAsUser(username),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.OK
        ).getEndpoint();
    }
}
