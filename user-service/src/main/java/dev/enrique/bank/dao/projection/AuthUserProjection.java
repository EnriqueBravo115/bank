package dev.enrique.bank.dao.projection;

import java.time.LocalDateTime;

public interface AuthUserProjection {
    Long getId();
    String getEmail();
    String getPhoneNumber();
    String getPhoneCode();
    String getCountryCode();
    String getGender();
    String getBirthday();
    LocalDateTime getRegistrationDate();
    Boolean isActive();
}
