package dev.enrique.bank.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsCorrect() {
        User user = new User(1L, "user", "Password123", "email@example.com",
                "1234567890", null, "México", "Male", null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
    }

    @Test
    void whenPasswordNotDefinedAndIncorrect() {
        User user = new User(1L, "user", "", "email@example.com",
                "1234567890", null, "México", "Male", null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).hasSize(2);

        List<String> constraintsMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(constraintsMessages)
                .contains("Invalid password")
                .contains("The password must be defined");
    }

    @Test
    void whenEmailNotDefinedAndInvalid() {
        User user = new User(1L, "user", "Password123", "",
                "1234567890", null, "México", "Male", null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).hasSize(2);

        List<String> constraintsMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(constraintsMessages)
                .contains("Invalid email")
                .contains("The email must be defined");
    }
}
