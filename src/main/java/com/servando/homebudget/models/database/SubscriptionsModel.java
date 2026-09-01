package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Subscriptions")
@Getter
public class SubscriptionsModel extends SharedProperties {
    @NotNull
    Double amount;
    @Nullable
    Integer billingDay;
    @NotNull
    @Indexed(direction = IndexDirection.ASCENDING)
    PriorityLevel priorityLevel;

    public SubscriptionsModel(String name, BillingCycle billingCycle, Double amount, @Nullable Integer billingDay, PriorityLevel priorityLevel) {
        super(name, billingCycle, Instant.now());
        this.priorityLevel = priorityLevel;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.billingDay = billingDay;
    }
}
