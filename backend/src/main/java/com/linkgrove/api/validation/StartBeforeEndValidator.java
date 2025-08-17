package com.linkgrove.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, Object> {
    private String startFieldName;
    private String endFieldName;

    @Override
    public void initialize(StartBeforeEnd annotation) {
        this.startFieldName = annotation.startField();
        this.endFieldName = annotation.endField();
        if (this.startFieldName == null || this.startFieldName.isBlank()) this.startFieldName = "startAt";
        if (this.endFieldName == null || this.endFieldName.isBlank()) this.endFieldName = "endAt";
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            String sName = (startFieldName == null || startFieldName.isBlank()) ? "startAt" : startFieldName;
            String eName = (endFieldName == null || endFieldName.isBlank()) ? "endAt" : endFieldName;
            Field startField = value.getClass().getDeclaredField(sName);
            Field endField = value.getClass().getDeclaredField(eName);
            startField.setAccessible(true);
            endField.setAccessible(true);
            Object startObj = startField.get(value);
            Object endObj = endField.get(value);
            if (startObj == null || endObj == null) return true;
            if (!(startObj instanceof LocalDateTime) || !(endObj instanceof LocalDateTime)) return true;
            LocalDateTime start = (LocalDateTime) startObj;
            LocalDateTime end = (LocalDateTime) endObj;
            return start.isBefore(end);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return true;
        }
    }
}


