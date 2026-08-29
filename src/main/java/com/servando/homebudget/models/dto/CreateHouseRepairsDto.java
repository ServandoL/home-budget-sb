package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.HouseRepairsStatus;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CreateHouseRepairsDto extends SharedRequestProperties {
    @NotBlank
    @NotNull
    String description;
    @NotNull
    Integer priority;
    @NotNull
    Integer cost;
    @NotNull
    HouseRepairsStatus status;
    @Nullable
    String notes;
    @Nullable
    String dateCompleted;
    @NotBlank
    @NotNull
    String category;
}
