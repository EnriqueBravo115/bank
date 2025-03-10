package dev.enrique.bank.dao.projection;

public interface UserPrincipalProjection {
    Long getId();

    String getEmail();

    String getActivationCode();
}
