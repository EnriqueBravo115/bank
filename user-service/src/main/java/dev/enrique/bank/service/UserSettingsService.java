package dev.enrique.bank.service;

import java.util.Map;

public interface UserSettingsService {
    String updateUsername(String username);
    Map<String, Object> updateEmail(String email);
    Map<String, Object> updatePhone(String countryCode, Long phone);
    String updateCountry(String country);
    String updateGender(String gender);
    String updateLanguage(String language);
}
