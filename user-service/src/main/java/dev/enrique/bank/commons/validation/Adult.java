package dev.enrique.bank.commons.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The date is a birthday of someone at least {@link #minAge} and at most {@link #maxAge} years old today. */
@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {
    String message() default "must be at least {minAge} years old";

    int minAge() default 18;

    int maxAge() default 120;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
