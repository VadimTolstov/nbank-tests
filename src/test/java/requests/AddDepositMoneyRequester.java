package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.DepositRequest;

import static io.restassured.RestAssured.given;

public class AddDepositMoneyRequester extends Request implements Postable<DepositRequest> {
    public AddDepositMoneyRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    private ValidatableResponse send(Object body) {
        RequestSpecification specification = given().spec(requestSpecification);
        if (body != null) {
            specification.body(body);
        }
        return specification.post("/api/v1/accounts/deposit")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(DepositRequest model) {
        return send(model);
    }


    // сырое тело — как строка, без сериализации через Jackson
    public void postRaw(String rawBody) {
        send(rawBody);
    }

    // PUT без body
    public void postNoBody() {
        send(null);
    }
}
