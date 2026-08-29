package com.servando.homebudget.models.dto;

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
    Integer cost;
    @Nullable
    HouseRepairsStatus status;
    @Nullable
    String notes;
    @Nullable
    String dateCompleted;
    @Nullable
    String category;
}
