package requests.skelethon;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public abstract class HttpRequest {
    protected final RequestSpecification requestSpecification;
    protected final Endpoint endpoint;
    protected final ResponseSpecification responseSpecification;

    protected HttpRequest(RequestSpecification requestSpecification,
                          Endpoint endpoint,
                          ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.endpoint = endpoint;
        this.responseSpecification = responseSpecification;
    }
}
