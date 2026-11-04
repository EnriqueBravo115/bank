package dev.enrique.bank.broker;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Topics {
    public static final String ACCOUNT_REQUESTED = "user-service.account-requested";
    public static final String USER_STATUS_CHANGED = "user-service.user-status-changed";
    public static final String USER_EMAIL_CHANGED = "user-service.user-email-changed";
}
