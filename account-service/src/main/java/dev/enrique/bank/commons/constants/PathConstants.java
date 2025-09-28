package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";
    public static final String API = "/api/v1";
    public static final String ACCOUNT_ID = "/{accountId}";

    // TRANSFER LIMITS
    public static final String GET_TRANSFER_LIMIT = ACCOUNT_ID + "/transfer-limit";
    public static final String CALCULATE_TRANSFER_FEE = "/transfer-fee";
    public static final String CHECK_SUFFICIENT_FUNDS = ACCOUNT_ID + "/has-sufficient-funds";
}
