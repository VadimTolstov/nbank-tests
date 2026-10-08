package requests;

import io.restassured.response.ValidatableResponse;
import models.BaseModel;

public interface Postable<T extends BaseModel> {
    ValidatableResponse post(T model);
}
