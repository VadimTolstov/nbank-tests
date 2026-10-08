package models.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;


@Builder(toBuilder = true)
public record DepositJsonResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("accountNumber")
        String accountNumber,
        @JsonProperty("balance")
        BigDecimal balance,
        @JsonProperty("depositAmount")
        BigDecimal depositAmount,
        @JsonProperty("transactionId")
        Long transactionId
) {
}
