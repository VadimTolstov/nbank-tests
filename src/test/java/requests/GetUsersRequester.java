package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UserRequest;

import static io.restassured.RestAssured.given;

public class GetUsersRequester extends Request implements Gettable {
    public GetUsersRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/admin/users")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}