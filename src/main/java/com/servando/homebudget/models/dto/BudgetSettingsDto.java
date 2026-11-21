package com.servando.homebudget.models.dto;

import java.time.Instant;

/**
 * The household's settings, or the defaults ({@code defaults: true}, no updatedAt or updatedBy)
 * while it hasn't saved any.
 */
public record BudgetSettingsDto(
        Double targetMonthlyTransfer,
        Double payPeriodBuffer,
        boolean defaults,
        Instant updatedAt,
        String updatedBy
) {
}
