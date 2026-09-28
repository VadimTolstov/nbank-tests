package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserJson(
        @JsonProperty("username")
        String username,
        @JsonProperty("password")
        String password,
        @JsonProperty("role")
        UserRole role

) {
}
