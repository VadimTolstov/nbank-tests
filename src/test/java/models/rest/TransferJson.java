package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;


@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferJson(
        @JsonProperty("senderAccountId")
        Long senderAccountId,
        @JsonProperty("receiverAccountId")
        Long receiverAccountId,
        @JsonProperty("amount")
        BigDecimal amount,
        @JsonProperty("message")
        String message
) {

}

