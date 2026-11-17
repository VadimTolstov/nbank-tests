package models.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record TransactionJson(
        @JsonProperty("id")
        Long id,
        @JsonProperty("amount")
        BigDecimal amount,
        @JsonProperty("type")
        String type,
        @JsonProperty("timestamp")
        String timestamp,
        @JsonProperty("relatedAccountId")
        Long relatedAccountId
) implements BaseModel {
}
