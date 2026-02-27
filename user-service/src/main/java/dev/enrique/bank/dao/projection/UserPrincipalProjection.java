package dev.enrique.bank.dao.projection;

// TODO: needs to exist a method returning specific user
public interface UserPrincipalProjection {
    Long getId();
    String getEmail();
    String getActivationCode();
}
