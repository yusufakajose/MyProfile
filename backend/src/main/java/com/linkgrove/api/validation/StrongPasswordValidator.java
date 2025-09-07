package com.linkgrove.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        if (value.length() < 8) return false;
        boolean hasLower = value.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = value.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return hasLower && hasUpper && hasDigit && hasSymbol;
    }
}


