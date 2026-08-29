package com.servando.homebudget.models.database;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("HouseRepairs")
@Getter
@Setter
public class HouseRepairsModel extends SharedProperties {
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
    String dateComplete;
    @NotBlank
    @NotNull
    String category;

    public HouseRepairsModel(String name, Instant updatedAt) {
        super(name, BillingCycle.OPTIONAL, updatedAt);
    }
}
