package com.site.meetingandclass.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Enforces a strong password:
 *   - 8 to 64 characters
 *   - at least one uppercase letter
 *   - at least one lowercase letter
 *   - at least one digit
 *   - at least one special character (non-alphanumeric)
 *   - no whitespace
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPassword.Validator.class)
public @interface StrongPassword {
    String message() default
        "Password must be 8-64 chars, contain upper, lower, digit and special character.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StrongPassword, String> {
        private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,64}$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext ctx) {
            return value != null && PATTERN.matcher(value).matches();
        }
    }
}
