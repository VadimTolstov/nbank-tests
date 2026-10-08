package models.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder(toBuilder = true)
public record UpdateUserNameRequest(
        @JsonProperty("name")
        String name
) {
}
