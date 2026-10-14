package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification,
                         Endpoint endpoint,
                         ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel body) {
        RequestSpecification spec = given().spec(requestSpecification);
        if (body != null) {
            spec = spec.body(body);
        }
        return spec
                .post(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(Long id) {
        return withId(id)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse update(Long id, BaseModel body) {
        return withId(id)
                .body(body)
                .patch(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Long id) {
        return withId(id)
                .delete(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    /**
     * Подставляет {@code {id}} в URL, только если шаблон его содержит.
     * Для endpoint'ов без {@code {id}} параметр игнорируется.
     */
    private RequestSpecification withId(long id) {
        RequestSpecification spec = given().spec(requestSpecification);
        if (endpoint.getEndpoint().contains("{id}")) {
            spec = spec.pathParam("id", id);
        }
        return spec;
    }
}

