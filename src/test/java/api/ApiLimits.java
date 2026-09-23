package api;

import java.math.BigDecimal;

public final class ApiLimits {
    private ApiLimits() {}

    public static final BigDecimal DEPOSIT_MAX = new BigDecimal("5000.00");
    public static final BigDecimal DEPOSIT_MIN = new BigDecimal("0.01");
    public static final BigDecimal TRANSFER_MAX = new BigDecimal("10000.00");
}
