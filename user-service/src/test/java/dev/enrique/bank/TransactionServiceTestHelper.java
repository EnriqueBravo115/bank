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
        TransactionDetailedProjection tProjection1 = factory.createProjection(TransactionDetailedProjection.class,
                new HashMap<>() {
                    {
                        put("id", 1L);
                        put("amount", new BigDecimal("100.50"));
                        put("transactionDate", LocalDateTime.now());
                        put("description", "transaction");
                        put("transactionType", "TRANSFER");
                    }
                });

        TransactionDetailedProjection tProjection2 = factory.createProjection(TransactionDetailedProjection.class,
                new HashMap<>() {
                    {
                        put("id", 2L);
                        put("amount", new BigDecimal("300.00"));
                        put("transactionDate", LocalDateTime.now());
                        put("description", "transaction");
                        put("transactionType", "TRANSFER");
                    }
                });
        return Arrays.asList(tProjection1, tProjection2);
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

    public static List<TransactionBasicProjection> createTransactionBasicProjections() {
        TransactionBasicProjection tProjection1 = factory.createProjection(TransactionBasicProjection.class,
                new HashMap<>() {
                    {
                        put("id", 1L);
                        put("amount", new BigDecimal("50.00"));
                        put("transactionType", TransactionType.TRANSFER);
                        put("transactionDate", LocalDateTime.now());
                    }
                });

        TransactionBasicProjection tProjection2 = factory.createProjection(TransactionBasicProjection.class,
                new HashMap<>() {
                    {
                        put("id", 2L);
                        put("amount", new BigDecimal("100.00"));
                        put("transactionType", TransactionType.TRANSFER);
                        put("transactionDate", LocalDateTime.now());
                    }
                });

        return Arrays.asList(tProjection1, tProjection2);
    }

    public static List<TransactionCommonProjection> createGroupedTransactions() {
        return Arrays.asList(
                createTransaction(2L, TransactionType.TRANSFER),
                createTransaction(2L, TransactionType.TRANSFER),
                createTransaction(2L, TransactionType.WITHDRAW));
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
