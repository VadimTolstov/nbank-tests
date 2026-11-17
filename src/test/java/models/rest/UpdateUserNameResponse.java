package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UpdateUserNameResponse(
        @JsonProperty("customer")
        CustomerProfileJsonResponse customer,
        @JsonProperty("message")
        String message
) implements BaseModel {
}
