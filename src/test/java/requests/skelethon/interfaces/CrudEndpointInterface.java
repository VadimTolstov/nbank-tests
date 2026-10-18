package requests.skelethon.interfaces;

import io.restassured.response.ValidatableResponse;
import models.rest.BaseModel;

public interface CrudEndpointInterface {
    Object post(BaseModel body);

    Object get(Long id);

    Object update(BaseModel body);

    Object delete(Long id);
}
