package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String AUTH_USER_ID_HEADER = "X-auth-user-id";
    public static final String PAGE_TOTAL_COUNT = "page-total-count";

    public static final String USER = "/api/v1/user";
    public static final String REGISTER = USER + "/register";
    public static final String PROFILE = USER + "/profile";
    public static final String KYC = USER + "/kyc";
    public static final String FINANCIAL = USER + "/financial-info";
}
