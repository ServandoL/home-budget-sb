package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Incomes")
public class IncomesModel {
    @NotNull
    Double netAmount;
    @NotNull
    PayFrequency frequency;

    public IncomesModel(PayFrequency frequency, Double netAmount) {
        this.frequency = frequency;
        this.netAmount = netAmount;
    }
}
