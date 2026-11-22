package com.servando.homebudget.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.servando.homebudget.models.database.HouseRepairsStatus;
import jakarta.annotation.Nullable;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UpdateHouseRepairsDto extends SharedRequestProperties {
    @Nullable
    String description;
    @Nullable
    Integer priority;
    @Nullable
    Double cost;
    @Nullable
    HouseRepairsStatus status;
    @Nullable
    String notes;
    @Nullable
    /**
     * Named like the stored field and what the UI sends. It used to be dateCompleted, which the UI
     * never sent, so completion dates were dropped; the old name still works.
     */
    @JsonAlias("dateCompleted")
    String dateComplete;
    @Nullable
    String category;
}
