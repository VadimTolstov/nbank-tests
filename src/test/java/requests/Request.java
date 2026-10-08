package requests;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public abstract class Request {
    protected RequestSpecification requestSpecification;
    protected ResponseSpecification responseSpecification;

    public Request(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }
}