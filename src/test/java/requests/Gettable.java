package requests;

import io.restassured.response.ValidatableResponse;

public interface Gettable {
    ValidatableResponse get();
}
