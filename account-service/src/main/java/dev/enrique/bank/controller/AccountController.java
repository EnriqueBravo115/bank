package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.dto.response.BalanceResponse;
import dev.enrique.bank.service.AccountService;
import dev.enrique.bank.service.CardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ACCOUNT)
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final CardService cardService;

    // The customer's own account, resolved from the JWT subject.
    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<AccountDetailedResponse> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.findByUserId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> getMyBalance(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.balanceOfUser(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.balanceOf(accountNumber));
    }

    // Lookups of any account: admins, and transaction-service to resolve identifiers and owners.
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @GetMapping("/get-by-account-number/{accountNumber}")
    public ResponseEntity<AccountDetailedResponse> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @GetMapping("/get-by-clabe/{clabe}")
    public ResponseEntity<AccountDetailedResponse> getByClabe(@PathVariable String clabe) {
        return ResponseEntity.ok(accountService.findByClabe(clabe));
    }

    // The account a card belongs to: transaction-service resolves CARD identifiers with it.
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @GetMapping("/get-by-card-number/{cardNumber}")
    public ResponseEntity<AccountDetailedResponse> getByCardNumber(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.findAccountByCardNumber(cardNumber));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<AccountDetailedResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(accountService.findByEmail(email));
    }
}
