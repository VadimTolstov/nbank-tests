package requests.skelethon;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public abstract class HttpRequest {
    protected RequestSpecification requestSpecification;
    //protected Endpoint endpoint;
    protected ResponseSpecification responseSpecification;

    public HttpRequest(RequestSpecification requestSpecification,  ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }
}
