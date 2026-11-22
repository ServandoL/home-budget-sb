package com.servando.homebudget.models.database;

import com.servando.homebudget.encryption.Encrypted;
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
    @Encrypted
    @NotBlank
    @NotNull
    String description;
    @Encrypted
    @NotBlank
    @NotNull
    String category;
    @NotNull
    Integer priority;
    @NotNull
    HouseRepairsStatus status;
    /** In dollars and cents. Older records saved whole dollars, which read back the same. */
    @Encrypted
    @Nullable
    Double cost;
    @Encrypted
    @Nullable
    String notes;
    @Nullable
    String dateComplete;

    public HouseRepairsModel(String name, Instant updatedAt) {
        super(name, BillingCycle.OPTIONAL, updatedAt);
    }
}
