package requests.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface {
    private final CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification,
                                  Endpoint endpoint,
                                  ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(
                requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T post(BaseModel body) {
        return (T) crudRequester.post(body).extract().as(endpoint.getResponseModel());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Long id) {
        return (T) crudRequester.get(id).extract().as(endpoint.getResponseModel());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T update(BaseModel body) {
        return (T) crudRequester.update(body).extract().as(endpoint.getResponseModel());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T delete(Long id) {
        return (T) crudRequester.delete(id);
    }
}
