package api.requests.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.BaseModel;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.GetEndpointInterface;

public class ValidatedProfileRequester<T extends BaseModel> extends HttpRequest implements GetEndpointInterface {
    private final GetProfileRequester getProfileRequester;

    public ValidatedProfileRequester(RequestSpecification requestSpecification,
                                     Endpoint endpoint,
                                     ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.getProfileRequester = new GetProfileRequester(
                requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getEndpoint() {
        return (T) getProfileRequester.getEndpoint().extract().as(endpoint.getResponseModel());
    }
}
