package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HouseRepairsStatus {
    COMPLETE("complete"),
    QUOTED("quoted"),
    SKIP("skip"),
    PENDING("pending");

    private final String value;

    HouseRepairsStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static HouseRepairsStatus fromValue(String value) {
        for (HouseRepairsStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
