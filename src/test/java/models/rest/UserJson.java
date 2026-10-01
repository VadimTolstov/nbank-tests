package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import generators.GeneratingRule;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserJson(
        @GeneratingRule(regex = "^[A-Za-z0-9]{3,15}$")
        @JsonProperty("username")
        String username,
        @GeneratingRule(regex = "^[A-Z]{3}[a-z]{4}[0-9]{3}[$%&]{2}$")
        @JsonProperty("password")
        String password,
        @GeneratingRule(regex = "^USER$")
        @JsonProperty("role")
        UserRole role
) {
}
