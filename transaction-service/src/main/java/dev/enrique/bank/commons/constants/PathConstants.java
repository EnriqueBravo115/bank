package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String PAGE_TOTAL_COUNT = "page-total-count";
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";
    public static final String API = "/api/v1";

    public static final String ACCOUNT_SERVICE = "account-service";

    public static final String TRANSACTION = API + "/transaction";
    public static final String TRANSACTION_QUERY = TRANSACTION + "/query";
    public static final String TRANSACTION_ANALYTICS = TRANSACTION + "/analytics";
    public static final String TRANSACTION_CREATION= TRANSACTION + "/creation";
    public static final String ACCOUNT_NUMBER = "/{sourceIdentifier}";

    // Controller "QUERY"
    public static final String GET_TRANSACTION_HISTORY = "/history" + ACCOUNT_NUMBER;
    public static final String GET_ALL_TRANSACTIONS = ACCOUNT_NUMBER;
    public static final String GET_TRANSACTIONS_BY_ACCOUNT_AND_YEAR = "/account-year";
    public static final String GET_TRANSACTIONS_FOR_ACCOUNTS = "/accounts";
    public static final String FIND_MAX_TRANSACTION = ACCOUNT_NUMBER + "/max-transaction";

    // Controller "ANALYTICS"
    public static final String GROUP_TRANSACTIONS_BY_TYPE = ACCOUNT_NUMBER + "/group-by-type";
    public static final String SUM_TRANSACTIONS_BY_TYPE = ACCOUNT_NUMBER + "/sum-by-type";
    public static final String GET_TRANSACTION_YEAR_STATS = ACCOUNT_NUMBER + "/year-stats";
    public static final String PARTITION_TRANSACTIONS_BY_AMOUNT = ACCOUNT_NUMBER + "/partition-by-amount";
    public static final String GET_TRANSACTION_TYPE_SUMMARY = ACCOUNT_NUMBER + "/type-summary";
    public static final String GET_TOTAL_TRANSACTION_AMOUNT = ACCOUNT_NUMBER + "/total-amount";
    public static final String GET_TOTAL_AMOUNT_BY_TYPE = ACCOUNT_NUMBER + "/total-amount-by-type";
    public static final String GET_AVERAGE_DAYS_BETWEEN = ACCOUNT_NUMBER + "/average-days-between";

    // Controller "SUPPORT"
    public static final String GET_UNIQUE_DESCRIPTIONS = ACCOUNT_NUMBER + "/unique-descriptions";
    public static final String GET_ALL_DESCRIPTIONS = ACCOUNT_NUMBER + "/all-descriptions";

    // Controller "CREATION"
    public static final String TRANSFER = "/transfer";
    public static final String PURCHASE = "/purchase";
    public static final String SERVICE_PAYMENT = "/service-payment";
}
