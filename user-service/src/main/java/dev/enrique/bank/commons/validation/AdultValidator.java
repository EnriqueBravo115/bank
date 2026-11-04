package dev.enrique.bank.commons.validation;

import java.time.LocalDate;
import java.time.Period;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {
    private int minAge;
    private int maxAge;

    @Override
    public void initialize(Adult annotation) {
        minAge = annotation.minAge();
        maxAge = annotation.maxAge();
    }

    @Override
    public boolean isValid(LocalDate birthday, ConstraintValidatorContext context) {
        if (birthday == null) {
            return true; // @NotNull decides whether it is required
        }
        int age = Period.between(birthday, LocalDate.now()).getYears();
        return !birthday.isAfter(LocalDate.now()) && age >= minAge && age <= maxAge;
    }
}
