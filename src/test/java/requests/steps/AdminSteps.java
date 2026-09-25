package requests.steps;

import generators.RandomModelGenerator;
import models.rest.CreateUserJsonRequest;
import models.rest.CreateUserJsonResponse;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class AdminSteps {
    public static CreateUserJsonRequest createUser() {
        CreateUserJsonRequest userRequest =
                RandomModelGenerator.generate(CreateUserJsonRequest.class);

        new ValidatedCrudRequester<CreateUserJsonResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        return userRequest;
    }
}
