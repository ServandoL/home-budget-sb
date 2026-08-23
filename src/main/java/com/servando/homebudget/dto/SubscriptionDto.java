package com.servando.homebudget.dto;

public record SubscriptionDto(
        String id,
        String name,
        Double amount,
        BillingCycle billingCycle,
        Integer billingDay
) {
}
