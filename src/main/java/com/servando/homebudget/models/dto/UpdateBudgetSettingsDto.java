package com.servando.homebudget.models.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** A null target means "calculate it". */
public record UpdateBudgetSettingsDto(
        @PositiveOrZero Double targetMonthlyTransfer,
        @NotNull @PositiveOrZero Double payPeriodBuffer
) {
}
