package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String API_V1 = "/api/v1";
    public static final String PAGE_TOTAL_COUNT = "page-total-count";
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";

    public static final String TRANSACTION = "/transaction";
    public static final String API_V1_TRANSACTION = API_V1 + TRANSACTION;

    public static final String ACCOUNT_ID = "/{accountId}";
    public static final String HISTORY = "/history";
    public static final String HISTORY_ID = HISTORY + ACCOUNT_ID;
    public static final String YEAR = "/{year}";
    public static final String YEAR_AND_ACCOUNT_ID = YEAR + ACCOUNT_ID;
    public static final String ALL = "/all";

    public static final String USERNAME = "/username";
    public static final String EMAIL = "/email";
    public static final String PHONE = "/phone";
    public static final String COUNTRY = "/country";
    public static final String GENDER = "/gender";
    public static final String LANGUAGE = "/language";
}
