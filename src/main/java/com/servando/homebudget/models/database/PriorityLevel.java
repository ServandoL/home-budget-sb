package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PriorityLevel {
    ESSENTIAL("essential"),
    IMPORTANT("important"),
    OPTIONAL("optional");

    private final String value;

    PriorityLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PriorityLevel fromValue(String value) {
        for (PriorityLevel priority : values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + value);
    }
}
