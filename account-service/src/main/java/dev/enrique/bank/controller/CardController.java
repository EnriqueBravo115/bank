package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.CARD;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.enrique.bank.commons.dto.response.CardResponse;
import dev.enrique.bank.commons.dto.response.IssuedCardResponse;
import dev.enrique.bank.service.CardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CARD)
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    // A debit card for the customer's own account, in the name of the token's holder. The response is
    // the only time the full number and the cvv are shown.
    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @PostMapping
    public ResponseEntity<IssuedCardResponse> issue(@AuthenticationPrincipal Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The token has no holder name");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.issue(jwt.getSubject(), name));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<List<CardResponse>> getMyCards(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cardService.findByUserId(jwt.getSubject()));
    }

    // The owner, or an admin for any card; someone else's card is 404.
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardResponse> block(@PathVariable Long cardId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cardService.block(cardId, ownerScope(jwt)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/{cardId}/unblock")
    public ResponseEntity<CardResponse> unblock(@PathVariable Long cardId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cardService.unblock(cardId, ownerScope(jwt)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/{cardId}/cancel")
    public ResponseEntity<CardResponse> cancel(@PathVariable Long cardId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cardService.cancel(cardId, ownerScope(jwt)));
    }

    private static String ownerScope(Jwt jwt) {
        boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return admin ? null : jwt.getSubject();
    }
}
