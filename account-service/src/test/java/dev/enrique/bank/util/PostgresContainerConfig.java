package dev.enrique.bank.util;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Singleton container: started once and shared by every test class, because Spring
// caches the application context across classes. Testcontainers' Ryuk removes it at the end.
public abstract class PostgresContainerConfig {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("account_db")
            .withUsername("user")
            .withPassword("password");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
