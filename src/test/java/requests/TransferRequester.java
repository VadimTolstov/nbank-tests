package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.TransferRequest;

import static io.restassured.RestAssured.given;

public class TransferRequester extends Request implements Postable<TransferRequest> {
    public TransferRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    private ValidatableResponse send(Object body) {
        RequestSpecification specification = given().spec(requestSpecification);
        if (body != null) {
            specification.body(body);
        }
        return specification.post("/api/v1/accounts/transfer")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(TransferRequest model) {
        return send(model);
    }

    // сырое тело — как строка, без сериализации через Jackson
    public void putRaw(String rawBody) {
        send(rawBody);
    }

    // PUT без body
    public void putNoBody() {
        send(null);
    }
}
