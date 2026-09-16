package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UpdateUserNameRequest;

import static io.restassured.RestAssured.given;

public class UpdateUserNameRequester extends Request implements Puttable<UpdateUserNameRequest> {
    public UpdateUserNameRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    private ValidatableResponse send(Object body) {
        RequestSpecification specification = given().spec(requestSpecification);
        if (body != null) {
            specification.body(body);
        }
        return specification.put("/api/v1/customer/profile")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(UpdateUserNameRequest model) {
        return send(model);
    }

    // сырое тело — как строка, без сериализации через Jackson
    public ValidatableResponse putRaw(String rawBody) {
        return send(rawBody);
    }

    // PUT без body
    public ValidatableResponse putNoBody() {
        return send(null);
    }
}
