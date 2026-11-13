package api.requests.skelethon.requesters;

import api.core.AuthContext;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.rest.CreateUserJsonRequest;
import models.rest.UserRole;
import org.jetbrains.annotations.Nullable;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.AuthEndpointInterface;

import static api.Headers.AUTH_HEADER;
import static io.restassured.RestAssured.given;

public class AuthRequester extends HttpRequest implements AuthEndpointInterface {


    public AuthRequester(RequestSpecification requestSpecification,
                         Endpoint endpoint,
                         ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }


    @Override
    public @Nullable String login(String username, String password) {
        String token = AuthContext.get(username);
        if (token == null) {
            Response response = given()
                    .spec(requestSpecification)
                    .body(new CreateUserJsonRequest(username, password, UserRole.USER))
                    .post(endpoint.getEndpoint())
                    .then()
                    .assertThat()
                    .spec(responseSpecification)
                    .extract()
                    .response();

            token = response.getHeader(AUTH_HEADER.getValue());

            if (token != null) {
                AuthContext.put(username, token);
            }
        }
        return token;
    }
}

