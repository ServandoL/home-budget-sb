package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PayFrequency {
    WEEKLY("weekly"),
    BI_WEEKLY("biweekly"),
    SEMI_MONTHLY("semimonthly");

    private final String value;

    PayFrequency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PayFrequency fromValue(String value) {
        for (PayFrequency frequency : values()) {
            if (frequency.value.equalsIgnoreCase(value)) {
                return frequency;
            }
        }
        throw new IllegalArgumentException("Unknown pay frequency: " + value);
    }
}
