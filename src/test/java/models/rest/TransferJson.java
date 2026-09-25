package models.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferJson extends BaseModel {
    @JsonProperty("senderAccountId")
    private Long senderAccountId;

    @JsonProperty("receiverAccountId")
    private Long receiverAccountId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("message")
    private String message;
}

