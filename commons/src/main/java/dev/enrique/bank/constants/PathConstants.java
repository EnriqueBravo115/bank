package dev.enrique.bank.constants;

public class PathConstants {
    public static final String API_V1 = "/api/v1";
    public static final String PAGE_TOTAL_COUNT = "page-total-count";
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";

    public static final String TRANSACTIONS = API_V1 + "/transactions";

    public static final String ACCOUNT = "/account";
    public static final String ACCOUNT_ID = ACCOUNT + "/{accountId}";

    public static final String GET_ALL_TRANSACTIONS = ACCOUNT_ID;
    public static final String GET_TRANSACTION_HISTORY = ACCOUNT_ID + "/history";
    public static final String GET_TRANSACTIONS_BY_YEAR = ACCOUNT_ID + "/year/{year}";
    public static final String GET_TRANSACTION_REVERSALS = ACCOUNT_ID + "/reversals";
    public static final String GET_TRANSACTIONS_FOR_ACCOUNTS = "/accounts";
    public static final String GROUP_TRANSACTIONS_BY_TYPE = ACCOUNT_ID + "/group-by-type";
    public static final String SUM_TRANSACTIONS_BY_TYPE = ACCOUNT_ID + "/sum-by-type";
    public static final String GET_TRANSACTION_YEAR_STATS = ACCOUNT_ID + "/year-stats";
    public static final String PARTITION_TRANSACTIONS_BY_AMOUNT = ACCOUNT_ID + "/partition-by-amount";
    public static final String GET_TRANSACTION_TYPE_SUMMARY = ACCOUNT_ID + "/type-summary";
    public static final String GET_TOTAL_TRANSACTION_AMOUNT = ACCOUNT_ID + "/total-amount";
    public static final String GET_TOTAL_AMOUNT_BY_TYPE = ACCOUNT_ID + "/total-by-type";
    public static final String CALCULATE_TRANSFER_FEE = "/transfer-fee";
    public static final String CHECK_SUFFICIENT_FUNDS = ACCOUNT_ID + "/has-sufficient-funds";
    public static final String GET_TRANSFER_LIMIT = ACCOUNT_ID + "/transfer-limit";
    public static final String GET_UNIQUE_DESCRIPTIONS = ACCOUNT_ID + "/unique-descriptions";
    public static final String GET_ALL_DESCRIPTIONS = ACCOUNT_ID + "/all-descriptions";
    public static final String GET_FORMATTED_AVERAGE_BALANCE = "/average-balance";
    public static final String FIND_MAX_TRANSACTION = ACCOUNT_ID + "/max-transaction";
    public static final String GET_AVERAGE_DAYS_BETWEEN = ACCOUNT_ID + "/average-days-between";
}
