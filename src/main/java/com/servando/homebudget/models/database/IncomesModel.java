package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Incomes")
@Getter
public class IncomesModel extends SharedProperties {
    @NotNull
    Double netAmount;
    @NotNull
    PayFrequency frequency;

    public IncomesModel(String name, BillingCycle billingCycle, PayFrequency frequency, Double netAmount) {
        super(name, billingCycle, Instant.now());
        this.frequency = frequency;
        this.netAmount = netAmount;
    }
}
