package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.CreateBalanceRequest;
import dev.enrique.bank.commons.dto.response.AccountBalanceResponse;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_BALANCE;

@RestController
@RequestMapping(ACCOUNT_BALANCE)
public class AccountBalanceController {
    //@PostMapping("/balance/{accountId}")
    //public ResponseEntity<AccountBalanceResponse> createAccountBalance(@PathVariable Long accountId,
    //        @RequestBody CreateBalanceRequest request) {
    //}
}
