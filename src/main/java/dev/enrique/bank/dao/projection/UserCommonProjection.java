package dev.enrique.bank.dao.projection;

public interface UserCommonProjection {
    Long getId();

    String getEmail();

    String getFullName();

    String getActivationCode();

    String getPasswordCode();
}
