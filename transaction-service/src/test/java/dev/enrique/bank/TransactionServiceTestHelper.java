package dev.enrique.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;

public class TransactionServiceTestHelper {
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
    private static final PageRequest pageable = PageRequest.of(0, 20);

    public static List<TransactionDetailedProjection> createTransactionDetailedProjection() {
        return Arrays.asList(
                createTransactionDetailedProjection("f47ac10b-58cc-4372-a567-0e02b2c3d479", new BigDecimal("100.50"),
                        "Test transaction 1", TransactionType.SERVICE, TransactionStatus.COMPLETED),
                createTransactionDetailedProjection("6ba7b810-9dad-11d1-80b4-00c04fd430c8", new BigDecimal("200.00"),
                        "Test transaction 2", TransactionType.TRANSFER, TransactionStatus.COMPLETED));
    }

    public static List<TransactionBasicProjection> createTransactionBasicProjectionList() {
        return Arrays.asList(
                createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.50")),
                createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")));
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

    public static TransactionDetailedProjection createTransactionDetailedProjection(String transactionCode,
            BigDecimal amount, String description, TransactionType type, TransactionStatus status) {
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

    private static TransactionCommonProjection createTransactionCommonProjection(BigDecimal amount,
            TransactionType type, TransactionStatus status) {
        return factory.createProjection(TransactionCommonProjection.class, new HashMap<String, Object>() {
            {
                put("amount", amount);
                put("transactionDate", LocalDateTime.now());
                put("transactionType", type);
                put("transactionStatus", status);
            }
        });
    }

    public static TransactionBasicProjection createTransactionBasicProjection(TransactionType type,
            BigDecimal amount) {
        return factory.createProjection(TransactionBasicProjection.class, new HashMap<String, Object>() {
            {
                put("transactionType", type);
                put("amount", amount);
            }
        });
    }
}
