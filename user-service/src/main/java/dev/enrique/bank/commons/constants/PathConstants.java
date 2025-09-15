package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";
    public static final String PAGE_TOTAL_COUNT = "page-total-count";

    public static final String API = "/api/v1";
    public static final String USER = "/user";
    public static final String AUTH = API + USER + "/auth";
    public static final String REGISTER = API + USER + "/register";

    // Controller AUTH
    public static final String LOGIN = "/login";
    public static final String FORGOT = "/forgot";
    public static final String RESET = "/reset";
    public static final String RESET_CODE = "/reset/{code}";
    public static final String RESET_CURRENT = "/reset/current";
    public static final String USER_EMAIL = "/user/{email}";

    // Controller REGISTER
    public static final String REGISTER_CHECK = REGISTER + "/check";
    public static final String REGISTER_CODE = REGISTER + "/code";
    public static final String REGISTER_ACTIVATE = REGISTER + "/activate/{code}";
    public static final String REGISTER_CONFIRM = REGISTER + "/confirm";
}
