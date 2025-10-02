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
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;

public class TransactionServiceTestHelper {
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
    private static final PageRequest pageable = PageRequest.of(0, 20);

    public static List<TransactionDetailedProjection> createTransactionDetailedProjection() {
        TransactionDetailedProjection projection1 = factory.createProjection(TransactionDetailedProjection.class,
                new HashMap<>() {
                    {
                        put("transactionCode", "f47ac10b-58cc-4372-a567-0e02b2c3d479");
                        put("amount", new BigDecimal("100.50"));
                        put("description", "test transaction 1");
                        put("transactionDate", LocalDateTime.now());
                        put("transactionType", TransactionType.PURCHASE);
                        put("transactionStatus", TransactionStatus.COMPLETED);
                    }
                });

        TransactionDetailedProjection projection2 = factory.createProjection(TransactionDetailedProjection.class,
                new HashMap<>() {
                    {
                        put("transactionCode", "6ba7b810-9dad-11d1-80b4-00c04fd430c8");
                        put("amount", new BigDecimal("200.00"));
                        put("description", "test transaction 2");
                        put("transactionDate", LocalDateTime.now());
                        put("transactionType", TransactionType.PURCHASE);
                        put("transactionStatus", TransactionStatus.COMPLETED);
                    }
                });

        return Arrays.asList(projection1, projection2);
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

    public static List<TransactionCommonProjection> createTransactionCommonProjections() {
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

        return Arrays.asList(tProjection1, tProjection2);
    }

    public static List<TransactionCommonProjection> createGroupedTransactions() {
        return Arrays.asList(
                createTransaction(2L, TransactionType.TRANSFER),
                createTransaction(2L, TransactionType.TRANSFER),
                createTransaction(2L, TransactionType.WITHDRAWAL));
    }

    private static TransactionCommonProjection createTransaction(Long id, TransactionType type) {
        return factory.createProjection(TransactionCommonProjection.class, new HashMap<String, Object>() {
            {
                put("id", id);
                put("amount", new BigDecimal("100.00"));
                put("transactionType", type);
                put("transactionDate", LocalDateTime.now());
                put("transactionStatus", TransactionStatus.COMPLETED);
            }
        });
    }
}
