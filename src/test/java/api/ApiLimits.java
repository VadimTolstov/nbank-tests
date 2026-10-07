package api;

import java.math.BigDecimal;

public final class ApiLimits {
    private ApiLimits() {
    }

    // ---------- compile-time String-константы для аннотаций ----------
    public static final String DEPOSIT_MAX_STR = "5000.00";
    public static final String DEPOSIT_MIN_STR = "0.01";
    public static final String MINIMUM_STEP_DEPOSIT_STR = "0.01";

    public static final String TRANSFER_MAX_STR = "10000.00";
    public static final String TRANSFER_MIN_STR = "0.01";
    public static final String MINIMUM_STEP_TRANSFER_STR = "0.01";

    // ---------- BigDecimal для рантайм-логики ----------
    public static final BigDecimal DEPOSIT_MAX = new BigDecimal(DEPOSIT_MAX_STR);
    public static final BigDecimal DEPOSIT_MIN = new BigDecimal(DEPOSIT_MIN_STR);
    public static final BigDecimal MINIMUM_STEP_DEPOSIT = new BigDecimal(MINIMUM_STEP_DEPOSIT_STR);

    public static final BigDecimal TRANSFER_MAX = new BigDecimal(TRANSFER_MAX_STR);
    public static final BigDecimal TRANSFER_MIN = new BigDecimal(TRANSFER_MIN_STR);
    public static final BigDecimal MINIMUM_STEP_TRANSFER = new BigDecimal(MINIMUM_STEP_TRANSFER_STR);
}
