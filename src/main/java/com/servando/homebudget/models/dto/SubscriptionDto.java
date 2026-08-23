package com.servando.homebudget.models.dto;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Subscriptions")
public record SubscriptionDto(
        @Id
        String id,
        @Nonnull
        @Indexed
        String name,
        @Nonnull
        Double amount,
        BillingCycle billingCycle,
        Integer billingDay
) {
}
