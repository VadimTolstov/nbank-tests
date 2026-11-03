package api.spec;

import api.core.AuthContext;
import config.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.rest.AdminConstants;

import java.util.List;

import static api.Headers.AUTH_HEADER;

public class RequestSpecs {
    private static final Config CFG = Config.getInstance();

    private RequestSpecs() {
    }

    private static RequestSpecBuilder defaultRequestBuilder() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(CFG.nbankUrl())
                .setBasePath(CFG.apiBasePathV1());
    }

    public static RequestSpecification defaultSpec() {
        return defaultRequestBuilder().build();
    }

    public static RequestSpecification adminSpec() {
        return authAsUser(AdminConstants.LOGIN);
    }

    public static RequestSpecification authAsUser(String username) {
        String token = AuthContext.get(username);
        if (token == null) {
            throw new IllegalStateException(
                    "Нет токена для пользователя '" + username);
        }
        return defaultRequestBuilder()
                .addHeader(AUTH_HEADER.getValue(), token)
                .build();
    }

    public static RequestSpecification fakeTokenSpec(String token) {
        return defaultRequestBuilder()
                .addHeader(AUTH_HEADER.getValue(), token)
                .build();
    }
}
