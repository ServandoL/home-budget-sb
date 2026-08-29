package com.servando.homebudget.models.database;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Debts")
@Getter
@Setter
public class DebtsModel extends SharedProperties {
    @Id
    String id;
    @NotBlank
    @NotNull
    @Indexed(unique = true)
    String name;
    @NotNull
    Double amount;
    @NotBlank
    @NotNull
    String from;
    @NotBlank
    @NotNull
    String description;
    @NotNull
    Instant debtStarted;
    @Nullable
    Instant datePaid;
    @NotNull
    Double amountPaid;

    @Nullable
    Instant createdAt;
    @Nullable
    Instant updatedAt;

    public DebtsModel(String name, BillingCycle billingCycle ) {
        super(name, billingCycle, Instant.now());
    }
}
