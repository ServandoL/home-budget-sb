package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Subscriptions")
public record SubscriptionsModel(
        @Id
        String id,
        @Nonnull
        @Indexed(unique = true)
        String name,
        @Nonnull
        Double amount,
        BillingCycle billingCycle,
        Integer billingDay,
        Instant createdAt,
        Instant updatedAt
) {
}
