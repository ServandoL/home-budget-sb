package com.servando.homebudget.models.database;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Debts")
@Getter
@Setter
public class DebtsModel extends SharedProperties {
    @NotNull
    Double amount;
    @NotBlank
    @NotNull
    String from;
    @NotBlank
    @NotNull
    String description;
    @NotNull
    String debtStarted;
    @Nullable
    String datePaid;
    @NotNull
    Double amountPaid;
    @NotNull
    Double amountOwed;

    public DebtsModel(String name, BillingCycle billingCycle ) {
        super(name, billingCycle, Instant.now());
    }
}
