package specs;

public final class ApiErrors {
    private ApiErrors() {
    }

    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ERROR = "error";

    public static final String BAD_REQUEST = "Bad Request";

    public static final class Profile {
        public static final String INVALID_NAME = "Name must contain two words with letters only";
    }

    public static final class Deposit {
        public static final String INVALID_AMOUNT = "Invalid account or amount";
        public static final String LIMIT_5000 = "Deposit amount exceeds the 5000 limit";
        public static final String INVALID_TYPES = "Invalid field types: accountId must be integer, amount must be number";
    }

    public static final class Transfer {
        public static final String INVALID = "Invalid transfer: insufficient funds or invalid accounts";
        public static final String LIMIT_10000 = "Transfer amount cannot exceed 10000";
    }

    public static final class Auth {
        public static final String UNAUTHORIZED_ACCOUNT = "Unauthorized access to account";
    }
}
