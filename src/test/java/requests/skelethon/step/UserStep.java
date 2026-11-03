package requests.skelethon.step;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import lombok.NonNull;
import models.rest.CustomerAccountJson;
import models.rest.CustomerProfileJsonResponse;
import org.jetbrains.annotations.Nullable;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.GetAccountsRequester;
import requests.skelethon.requesters.ValidatedProfileRequester;

public class UserStep {

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
