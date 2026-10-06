package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CustomerAccountJson(
        @JsonProperty("id")
        Long id,
        @JsonProperty("accountNumber")
        String accountNumber,
        @JsonProperty("balance")
        BigDecimal balance
) {
}
