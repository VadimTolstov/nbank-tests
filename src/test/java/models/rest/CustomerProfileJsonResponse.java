package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CustomerProfileJsonResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("username")
        String username,
        @JsonProperty("name")
        String name,
        @JsonProperty("role")
        UserRole role
) {
}