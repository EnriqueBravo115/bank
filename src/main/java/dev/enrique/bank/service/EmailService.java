package dev.enrique.bank.service;

public interface EmailService {
    public void sendVerificationEmail(String to, String subject, String body);
}
