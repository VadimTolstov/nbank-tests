package api.requests.skelethon.requesters;


import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.CreateUserJsonResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.GetEndpointInterface;

import java.util.List;

import static io.restassured.RestAssured.given;

public class GetUsersRequester extends HttpRequest implements GetEndpointInterface {
    public GetUsersRequester(RequestSpecification requestSpecification,
                                Endpoint endpoint,
                                ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public List<CreateUserJsonResponse> getEndpoint() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification)
                .extract()
                .as(new TypeRef<>() {});
    }
}
