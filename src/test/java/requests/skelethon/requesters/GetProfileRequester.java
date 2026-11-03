package requests.skelethon.requesters;


import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.CustomerAccountJson;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.GetEndpointInterface;

import java.util.List;

import static io.restassured.RestAssured.given;

public class GetProfileRequester extends HttpRequest implements GetEndpointInterface {
    public GetProfileRequester(RequestSpecification requestSpecification,
                               Endpoint endpoint,
                               ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse getEndpoint() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
