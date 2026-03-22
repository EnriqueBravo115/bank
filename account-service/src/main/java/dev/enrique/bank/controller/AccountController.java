package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.service.AccountService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ACCOUNT)
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-account-number/{accountNumber}")
    public ResponseEntity<AccountDetailedResponse> getByAccountNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }
}
