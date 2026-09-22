package specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    private ResponseSpecs() {
    }

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    private static ResponseSpecification withBodyError(int status, String errorKey, String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(status)
                .expectBody(errorKey, Matchers.equalTo(errorValue))
                .build();
    }

    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequest(String errorKey, String errorValue) {
        return withBodyError(HttpStatus.SC_BAD_REQUEST, errorKey, errorValue);
    }


    public static ResponseSpecification requestReturnsUnauthorizedRequest() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_UNAUTHORIZED)
                .build();
    }

    public static ResponseSpecification nameIsInvalid() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Profile.INVALID_NAME);
    }

    public static ResponseSpecification requestIsMalformed() {
        return requestReturnsBadRequest(ApiErrors.KEY_ERROR, ApiErrors.BAD_REQUEST);
    }

    public static ResponseSpecification requestReturnsForbidden() {
        return withBodyError(HttpStatus.SC_FORBIDDEN, ApiErrors.KEY_MESSAGE, ApiErrors.Auth.UNAUTHORIZED_ACCOUNT);
    }

    public static ResponseSpecification fieldTypesAreInvalid() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Deposit.INVALID_TYPES);
    }

    public static ResponseSpecification amountOrAccountIsInvalid() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Deposit.INVALID_AMOUNT);
    }

    public static ResponseSpecification depositLimitExceeded() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Deposit.LIMIT_5000);
    }

    public static ResponseSpecification transferLimitExceeded() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Transfer.LIMIT_10000);
    }

    public static ResponseSpecification transferIsInvalid() {
        return requestReturnsBadRequest(ApiErrors.KEY_MESSAGE, ApiErrors.Transfer.INVALID);
    }
}