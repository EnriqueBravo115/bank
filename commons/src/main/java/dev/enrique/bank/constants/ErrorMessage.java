package dev.enrique.bank.constants;

public class ErrorMessage {
    public static final String USER_NOT_FOUND = "User not found";
    public static final String JWT_TOKEN_EXPIRED = "JWT token is expired or invalid";

    public static final String EMAIL_NOT_VALID = "Please enter a valid email address.";
    public static final String EMAIL_NOT_FOUND = "Email not found";
    public static final String EMAIL_HAS_ALREADY_BE_TAKEN = "Email has already be taken";
    public static final String EMPTY_PASSWORD = "Password cannot be empty.";
    public static final String INVALID_PASSWORD_RESET_CODE = "Password reset code is invalid";

    public static final String SHORT_PASSWORD = "Your password needs to be at least 8 characters. Please enter a longer one.";
    public static final String EMPTY_CURRENT_PASSWORD = "Current password cannot be empty.";
    public static final String EMPTY_PASSWORD_CONFIRMATION = "Password confirmation cannot be empty.";
    public static final String PASSWORD_LENGTH_ERROR = "Your password needs to be at least 8 characters";
    public static final String BLANK_NAME = "What's your name?";
    public static final String NAME_NOT_VALID = "Please enter a valid name";
    public static final String ACTIVATION_CODE_NOT_FOUND = "Activation code not found";

    public static final String ACCOUNT_NOT_FOUND = "Account not found";

    public static final String TRANSACTION_NOT_FOUND = "Transaction not found";
    public static final String SCHEDULED_TRANSFER_NOT_FOUND = "Scheduled transfer not found";
    public static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    public static final String FAILED_SCHEDULED_TRANSFER = "Failed to schedule transfer";
}
