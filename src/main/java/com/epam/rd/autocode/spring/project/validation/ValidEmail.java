package com.epam.rd.autocode.spring.project.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Email;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @Email accepts "user@email" (a domain without a dot is valid by RFC), so the regexp is added:
// the domain must contain a dot and the last part must have at least 2 characters.
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}$")
@ReportAsSingleViolation
public @interface ValidEmail {
    String message() default "{validation.email.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
