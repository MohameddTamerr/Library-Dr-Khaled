package com.library.pos.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute == Role.DELIVERY_MEN) {
            return "delivery_men";
        }
        return attribute.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim();
        if (normalized.equalsIgnoreCase("delivery_men") || normalized.equalsIgnoreCase("delivery_man")
                || normalized.equalsIgnoreCase("delivery")) {
            return Role.DELIVERY_MEN;
        }
        if (normalized.equalsIgnoreCase("owner")) {
            return Role.OWNER;
        }
        if (normalized.equalsIgnoreCase("worker")) {
            return Role.WORKER;
        }
        try {
            return Role.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
