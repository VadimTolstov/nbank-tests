package api.requests.skelethon.requesters;


import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.GetEndpointInterface;

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
