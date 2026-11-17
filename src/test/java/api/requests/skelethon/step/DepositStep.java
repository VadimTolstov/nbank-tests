package api.requests.skelethon.step;

import api.ApiErrors;
import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import lombok.NonNull;
import models.rest.DepositJsonRequest;
import models.rest.DepositJsonResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;

public class DepositStep {

    public static DepositJsonResponse deposit(@NonNull String username, @NonNull DepositJsonRequest deposit) {
        return (DepositJsonResponse) new ValidatedCrudRequester<>(
                RequestSpecs.authAsUser(username),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.OK
        ).post(deposit);
    }

    public static void depositExpectingError(@NonNull String username, @NonNull DepositJsonRequest depositJsonRequest, int expectedStatus, @NonNull String errorMessage) {
        new CrudRequester(
                RequestSpecs.authAsUser(username),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.errorWithField(
                        expectedStatus,
                        ApiErrors.KEY_MESSAGE,
                        errorMessage
                )).post(depositJsonRequest);
    }
}
