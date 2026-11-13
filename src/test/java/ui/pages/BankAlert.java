package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    USER_UPDATED_NAME_SUCCESS("✅ Name updated successfully!"),
    USER_UPDATED_NAME_FAIL("❌ Please enter a valid name."),
    USER_CREATED_DEPOSIT_SUCCESSFULLY("✅ Successfully deposited $%s to account %s!"),
    USER_CREATED_DEPOSIT_FAIL("❌ Please enter a valid amount."),
    USER_TRANSFER_SUCCESSFULLY("✅ Successfully transferred $%s to account %s!"),
    TRANSFER_FAILED_INSUFFICIENT_FUNDS("❌ Error: Invalid transfer: insufficient funds or invalid accounts");


    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}