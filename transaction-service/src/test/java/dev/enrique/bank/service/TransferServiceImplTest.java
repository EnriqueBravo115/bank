//package dev.enrique.bank.service;
//
//import static dev.enrique.bank.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
//import static dev.enrique.bank.constants.ErrorMessage.INSUFFICIENT_FUNDS;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//
//import dev.enrique.bank.enums.TransactionStatus;
//import dev.enrique.bank.exception.ApiRequestException;
//import dev.enrique.bank.dao.AccountRepository;
//import dev.enrique.bank.dao.TransactionRepository;
//import dev.enrique.bank.model.Account;
//import dev.enrique.bank.model.Transaction;
//import dev.enrique.bank.service.impl.TransferServiceImpl;
//import dev.enrique.bank.service.util.TransactionHelper;
//
//@ExtendWith(MockitoExtension.class)
//public class TransferServiceImplTest {
//    @Mock
//    private TransactionRepository transactionRepository;
//    @Mock
//    private AccountRepository accountRepository;
//    @Mock
//    private TransactionHelper transactionHelper;
//
//    @InjectMocks
//    private TransferServiceImpl transferService;
//
//    private Account sourceAccount;
//    private Account targetAccount;
//
//    @BeforeEach
//    void setUp() {
//        sourceAccount = Account.builder()
//                .id(1L)
//                .sourceIdentifier("123")
//                .balance(new BigDecimal("200.00"))
//                .build();
//
//        targetAccount = Account.builder()
//                .id(2L)
//                .sourceIdentifier("456")
//                .balance(new BigDecimal("100.00"))
//                .build();
//    }
//
//    @Test
//    void transfer_ShouldCompleteSuccessfully_WhenValidData() {
//        BigDecimal amount = new BigDecimal("100.00");
//
//        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
//        when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));
//
//        transferService.transfer(sourceAccount.getId(), targetAccount.getId(), amount);
//
//        assertEquals(new BigDecimal("100.00"), sourceAccount.getBalance());
//        assertEquals(new BigDecimal("200.00"), targetAccount.getBalance());
//        verify(transactionRepository).save(any(Transaction.class));
//    }
//
//    @Test
//    void transfer_ShouldThrowException_WhenSourceAccountNotFound() {
//        Long sourceAccountId = 1L;
//        Long targetAccountId = 2L;
//        BigDecimal amount = new BigDecimal("100.00");
//
//        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.empty());
//
//        ApiRequestException exception = assertThrows(ApiRequestException.class,
//                () -> transferService.transfer(sourceAccountId, targetAccountId, amount));
//
//        assertEquals(ACCOUNT_NOT_FOUND, exception.getMessage());
//        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
//    }
//
//    @Test
//    void transfer_ShouldThrowException_WhenInsufficientFunds() {
//        BigDecimal amount = new BigDecimal("300.00");
//
//        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
//        when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));
//
//        ApiRequestException exception = assertThrows(ApiRequestException.class,
//                () -> transferService.transfer(sourceAccount.getId(), targetAccount.getId(), amount));
//
//        assertEquals(INSUFFICIENT_FUNDS, exception.getMessage());
//        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
//    }
//
//    @Test
//    void reverseTransfer_ShouldCompleteSuccessfully_WhenValidTransaction() {
//        Long transactionId = 1L;
//        BigDecimal amount = new BigDecimal("50.00");
//
//        Transaction transaction = Transaction.builder()
//                .id(transactionId)
//                .amount(amount)
//                .sourceAccount(sourceAccount)
//                .targetAccount(targetAccount)
//                .transactionStatus(TransactionStatus.COMPLETED)
//                .build();
//
//        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//
//        transferService.reverseTransfer(transactionId);
//
//        assertEquals(new BigDecimal("250.00"), sourceAccount.getBalance());
//        assertEquals(new BigDecimal("50.00"), targetAccount.getBalance());
//        assertEquals(TransactionStatus.REVERSED, transaction.getTransactionStatus());
//        verify(transactionRepository).save(any(Transaction.class));
//    }
//}
