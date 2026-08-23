package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BillCategory {
    RENT("rent"),
    UTILITY("utility"),
    INSURANCE("insurance"),
    OTHER("other");

    private final String value;

    BillCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BillCategory fromValue(String value) {
        for (BillCategory category : values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown bill category: " + value);
    }
}
