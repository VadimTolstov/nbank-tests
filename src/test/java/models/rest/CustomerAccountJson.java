package models.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountJson extends BaseModel {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
}
