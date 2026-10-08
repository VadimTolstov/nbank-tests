package requests;

import io.restassured.response.ValidatableResponse;
import models.BaseModel;

public interface Puttable <T extends BaseModel> {
    ValidatableResponse put(T model);
}
