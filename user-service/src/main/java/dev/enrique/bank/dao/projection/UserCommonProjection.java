package dev.enrique.bank.dao.projection;

public interface UserCommonProjection {
    Long getId();
    String getEmail();
    String getFirstSurname();
    String getSecondSurname();
    String getActivationCode();
    String getPasswordCode();
}
