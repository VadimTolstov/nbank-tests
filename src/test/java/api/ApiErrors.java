package api;

public final class ApiErrors {

    private ApiErrors() {
    }

    // ---------- ключи в теле ответа ----------

    public static final String KEY_MESSAGE  = "message";
    public static final String KEY_ERROR    = "error";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";

    // ---------- общие сообщения ----------

    public static final String BAD_REQUEST = "Bad Request";

    // ---------- валидация полей пользователя ----------

    public static final class User {

        private User() {
        }

        // username
        public static final String USERNAME_BLANK =
                "Username cannot be blank";

        public static final String USERNAME_LENGTH =
                "Username must be between 3 and 15 characters";

        public static final String USERNAME_INVALID_CHARS =
                "Username must contain only letters, digits, dashes, underscores, and dots";

        // password
        public static final String PASSWORD_WEAK =
                "Password must contain at least one digit, one lower case, "
                        + "one upper case, one special character, no spaces, "
                        + "and be at least 8 characters long";

        public static final String PASSWORD_BLANK =
                "Password cannot be blank";
    }

    // ---------- профиль ----------

    public static final class Profile {

        private Profile() {
        }

        public static final String INVALID_NAME =
                "Name must contain two words with letters only";
    }

    // ---------- депозит ----------

    public static final class Deposit {

        private Deposit() {
        }

        public static final String INVALID_AMOUNT = "Invalid account or amount";
        public static final String LIMIT_5000 = "Deposit amount exceeds the 5000 limit";
        public static final String INVALID_TYPES =
                "Invalid field types: accountId must be integer, amount must be number";
    }

    // ---------- перевод ----------

    public static final class Transfer {

        private Transfer() {
        }

        public static final String INVALID =
                "Invalid transfer: insufficient funds or invalid accounts";
        public static final String LIMIT_10000 = "Transfer amount cannot exceed 10000";
    }

    // ---------- авторизация ----------

    public static final class Auth {

        private Auth() {
        }

        public static final String UNAUTHORIZED_ACCOUNT =
                "Unauthorized access to account";
    }
}