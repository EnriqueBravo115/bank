package dev.enrique.bank.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.dto.response.CardResponse;
import dev.enrique.bank.commons.dto.response.IssuedCardResponse;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.CardStatus;
import dev.enrique.bank.commons.enums.CardType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotOperableException;
import dev.enrique.bank.commons.exception.CardDeclinedException;
import dev.enrique.bank.commons.exception.CardNotFoundException;
import dev.enrique.bank.commons.exception.CardStateException;
import dev.enrique.bank.commons.util.CardNumberGenerator;
import dev.enrique.bank.commons.util.CardSecurityCode;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.CardRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Card;
import dev.enrique.bank.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    static final int VALIDITY_YEARS = 5;
    // An account has at most one card in use; a new one is issued after cancelling it.
    private static final Set<CardStatus> IN_USE = EnumSet.of(CardStatus.ACTIVE, CardStatus.BLOCKED);

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardSecurityCode cardSecurityCode;
    private final Clock clock;

    @Override
    @Transactional
    public IssuedCardResponse issue(String userId, String cardHolderName) {
        Account found = accountRepository.findEntityByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("userId", userId));
        // Locked so two requests at once cannot both issue a card
        Account account = accountRepository.lockByAccountNumber(found.getAccountNumber()).orElseThrow();
        if (account.getAccountStatus() != AccountStatus.OPEN) {
            throw new AccountNotOperableException(account.getAccountNumber(), account.getAccountStatus());
        }
        if (cardRepository.existsByAccountIdAndCardStatusIn(account.getId(), IN_USE)) {
            throw new CardStateException("The account already has a card; cancel it to issue a new one");
        }

        Card card = cardRepository.save(Card.builder()
                .account(account)
                .cardNumber(uniqueCardNumber())
                .cardHolderName(cardHolderName.trim().toUpperCase(Locale.ROOT))
                .cardType(CardType.DEBIT)
                .cardStatus(CardStatus.ACTIVE)
                .expirationDate(YearMonth.now(clock).plusYears(VALIDITY_YEARS).atEndOfMonth())
                .build());
        log.info("Card {} issued for account {}", card.getId(), account.getAccountNumber());

        return new IssuedCardResponse(card.getId(), card.getCardNumber(), card.getCardHolderName(),
                card.getExpirationDate(), cardSecurityCode.of(card.getCardNumber(), card.getExpirationDate()),
                card.getCardType(), card.getCardStatus());
    }

    @Override
    public List<CardResponse> findByUserId(String userId) {
        return cardRepository.findAllByUserId(userId).stream().map(CardResponse::from).toList();
    }

    @Override
    @Transactional
    public CardResponse block(Long cardId, String ownerScope) {
        return change(cardId, ownerScope, CardStatus.BLOCKED, EnumSet.of(CardStatus.ACTIVE));
    }

    @Override
    @Transactional
    public CardResponse unblock(Long cardId, String ownerScope) {
        return change(cardId, ownerScope, CardStatus.ACTIVE, EnumSet.of(CardStatus.BLOCKED));
    }

    @Override
    @Transactional
    public CardResponse cancel(Long cardId, String ownerScope) {
        return change(cardId, ownerScope, CardStatus.CANCELLED, IN_USE);
    }

    @Override
    public AccountDetailedResponse findAccountByCardNumber(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber).orElseThrow(CardNotFoundException::new);
        return accountRepository.findByAccountNumber(card.getAccount().getAccountNumber()).orElseThrow();
    }

    @Override
    public void verifyPayment(Account account, String cardNumber, String cvv) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .orElseThrow(() -> new CardDeclinedException("the card is not linked to the account"));
        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new CardDeclinedException("the card is " + card.getCardStatus());
        }
        if (card.isExpired(LocalDate.now(clock))) {
            throw new CardDeclinedException("the card is expired");
        }
        if (!cardSecurityCode.matches(card.getCardNumber(), card.getExpirationDate(), cvv)) {
            throw new CardDeclinedException("invalid security code");
        }
    }

    // Changing to the current status is a no-op, so a repeated request gets the same answer.
    private CardResponse change(Long cardId, String ownerScope, CardStatus target, Set<CardStatus> allowedFrom) {
        Card card = cardRepository.findWithAccountById(cardId)
                .filter(c -> ownerScope == null || ownerScope.equals(c.getAccount().getUserId()))
                .orElseThrow(CardNotFoundException::new);
        if (card.getCardStatus() != target) {
            if (!allowedFrom.contains(card.getCardStatus())) {
                throw new CardStateException("Card is " + card.getCardStatus() + " and cannot change to " + target);
            }
            card.setCardStatus(target);
            log.info("Card {} is now {}", cardId, target);
        }
        return CardResponse.from(card);
    }

    private String uniqueCardNumber() {
        String number;
        do {
            number = cardNumberGenerator.generate();
        } while (cardRepository.existsByCardNumber(number));
        return number;
    }
}
