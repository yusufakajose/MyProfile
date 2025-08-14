package com.linkgrove.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class AliasNotReservedValidator implements ConstraintValidator<AliasNotReserved, String> {
    private static final Set<String> RESERVED = Set.of(
            "admin", "root", "login", "register", "api", "r", "u", "settings", "webhooks"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String v = value.trim().toLowerCase();
        return !RESERVED.contains(v);
    }
}


