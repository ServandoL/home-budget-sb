package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Subscriptions")
public class SubscriptionsModel extends SharedProperties {
    @NotNull
    Double amount;
    @Nullable
    BillingCycle billingCycle;
    @Nullable
    Integer billingDay;

    public SubscriptionsModel(@org.jspecify.annotations.Nullable Instant createdAt, String name, @org.jspecify.annotations.Nullable Instant updatedAt, Double amount, @Nullable BillingCycle billingCycle, @Nullable Integer billingDay) {
        super(createdAt, name, updatedAt);
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.billingDay = billingDay;
    }
}
