package dev.enrique.bank.commons.constants;

public class PathConstants {
    public static final String USER = "/api/v1/user";
    public static final String REGISTER = USER + "/register";
    // Relative to USER
    public static final String ME_EMAIL = "/me/email";
    public static final String PROFILE = USER + "/profile";
    public static final String KYC = USER + "/kyc";
    public static final String FINANCIAL = USER + "/financial-info";
    public static final String OUTBOX = USER + "/outbox";
}
