package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BillingCycle {
    MONTHLY("monthly"),
    ANNUAL("annual");
    private final String value;

    BillingCycle(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BillingCycle fromValue(String value) {
        for (BillingCycle cycle : values()) {
            if (cycle.value.equalsIgnoreCase(value)) {
                return cycle;
            }
        }
        throw new IllegalArgumentException("Unknown billing cycle: " + value);
    }
}
