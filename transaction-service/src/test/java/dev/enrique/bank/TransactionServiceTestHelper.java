package dev.enrique.bank;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static dev.enrique.bank.commons.constants.TestConstants.CLABE;
import static dev.enrique.bank.commons.constants.TestConstants.STATUS_COMPLETED;

public class TransactionServiceTestHelper {
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
    private static final PageRequest pageable = PageRequest.of(0, 20);

    public static List<TransactionDetailedProjection> generateDetailedProjections() {
        return List.of(
                createTransactionDetailedProjection(CLABE, new BigDecimal("100.00"),
                        "Test transaction 1", TransactionType.SERVICE, STATUS_COMPLETED),
                createTransactionDetailedProjection(CLABE, new BigDecimal("200.00"),
                        "Test transaction 2", TransactionType.SERVICE, STATUS_COMPLETED),
                createTransactionDetailedProjection(CLABE, new BigDecimal("100.00"),
                        "Test transaction 3", TransactionType.TRANSFER, STATUS_COMPLETED),
                createTransactionDetailedProjection(CLABE, new BigDecimal("200.00"),
                        "Test transaction 4", TransactionType.TRANSFER, STATUS_COMPLETED));
    }

    public static List<TransactionCommonProjection> generateCommonProjections() {
        return List.of(
                createTransactionCommonProjection(new BigDecimal("200"),
                        TransactionType.TRANSFER, STATUS_COMPLETED, LocalDateTime.of(2025, 10, 6, 5, 5)),
                createTransactionCommonProjection(new BigDecimal("200"),
                        TransactionType.TRANSFER, STATUS_COMPLETED, LocalDateTime.of(2025, 10, 8, 5, 5)),
                createTransactionCommonProjection(new BigDecimal("200"),
                        TransactionType.TRANSFER, STATUS_COMPLETED, LocalDateTime.of(2025, 10, 10, 5, 5))
        );
    }

    public static List<TransactionBasicProjection> generateBasicProjections() {
        return List.of(
                createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("100.00")),
                createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("200.00")),
                createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")));
    }

    public static Page<TransactionCommonProjection> createTransactionPageCommonProjections() {
        TransactionCommonProjection tProjection1 = factory.createProjection(TransactionCommonProjection.class,
                new HashMap<>() {
                    {
                        put("id", 1L);
                        put("amount", new BigDecimal("150.75"));
                        put("description", "transaction");
                        put("transactionDate", LocalDateTime.now());
                        put("transactionType", TransactionType.TRANSFER);
                        put("transactionStatus", TransactionStatus.COMPLETED);
                    }
                });

        TransactionCommonProjection tProjection2 = factory.createProjection(TransactionCommonProjection.class,
                new HashMap<>() {
                    {
                        put("id", 2L);
                        put("amount", new BigDecimal("150.75"));
                        put("description", "transaction");
                        put("transactionDate", LocalDateTime.now());
                        put("transactionType", TransactionType.TRANSFER);
                        put("transactionStatus", TransactionStatus.COMPLETED);
                    }
                });
        return new PageImpl<>(Arrays.asList(tProjection1, tProjection2), pageable, 20);
    }

    public static TransactionDetailedProjection createTransactionDetailedProjection(String transactionCode, BigDecimal amount,
                                                                                    String description, TransactionType type,
                                                                                    TransactionStatus status) {
        return factory.createProjection(TransactionDetailedProjection.class, new HashMap<String, Object>() {
            {
                put("transactionCode", transactionCode);
                put("amount", amount);
                put("description", description);
                put("transactionType", type);
                put("transactionStatus", status);
                put("transactionDate", LocalDateTime.now());
            }
        });
    }

    public static TransactionCommonProjection createTransactionCommonProjection(BigDecimal amount, TransactionType type, TransactionStatus status, LocalDateTime localDateTime) {
        return factory.createProjection(TransactionCommonProjection.class, new HashMap<String, Object>() {
            {
                put("amount", amount);
                put("transactionDate", localDateTime);
                put("transactionType", type);
                put("transactionStatus", status);
            }
        });
    }

    public static TransactionBasicProjection createTransactionBasicProjection(TransactionType type, BigDecimal amount) {
        return factory.createProjection(TransactionBasicProjection.class, new HashMap<String, Object>() {
            {
                put("transactionType", type);
                put("amount", amount);
            }
        });
    }
}
