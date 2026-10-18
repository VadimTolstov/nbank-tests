package requests.skelethon.step;

import api.ApiErrors;
import api.spec.RequestSpecs;
import api.spec.ResponseSpecs;
import lombok.NonNull;
import models.rest.DepositJsonRequest;
import models.rest.TransferJson;
import org.apache.http.HttpStatus;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

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
                ResponseSpecs.errorWithField(
                        HttpStatus.SC_BAD_REQUEST,
                        ApiErrors.KEY_MESSAGE,
                        errorMessage
                )).post(transfer);
    }
}
