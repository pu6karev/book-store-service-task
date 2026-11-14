package com.epam.rd.autocode.spring.project.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    // at least 8 characters, a lowercase letter, an uppercase letter, a digit, and a special character
    // (not a letter, not a digit, and not a space)
    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\d\\s]).{8,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank checks for empty values, ensuring that two messages don't appear at the same time.
        return value == null || value.isBlank() || PATTERN.matcher(value).matches();
    }
}
