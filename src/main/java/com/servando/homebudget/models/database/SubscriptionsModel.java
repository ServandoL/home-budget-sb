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

@Document("Subscriptions")
@Getter
public class SubscriptionsModel extends SharedProperties {
    @NotNull
    Double amount;
    @Nullable
    Integer billingDay;

    public SubscriptionsModel(String name, BillingCycle billingCycle, Double amount, @Nullable Integer billingDay) {
        super(name, billingCycle, Instant.now());
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.billingDay = billingDay;
    }
}
