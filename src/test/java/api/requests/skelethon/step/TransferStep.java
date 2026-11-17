package api.requests.skelethon.step;

import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import lombok.NonNull;
import models.rest.TransferJson;
import org.apache.http.HttpStatus;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;

public class TransferStep {

    public static void transfer(@NonNull String username, @NonNull TransferJson transfer) {
        new ValidatedCrudRequester<>(
                RequestSpecs.authAsUser(username),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.OK
        ).post(transfer);
    }

    public static void transferExpectingError(@NonNull String username, @NonNull TransferJson transfer, @NonNull String errorMessage) {
        new CrudRequester(
                RequestSpecs.authAsUser(username),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.errorWithText(
                        HttpStatus.SC_BAD_REQUEST,
                        errorMessage
                )).post(transfer);
    }
}
