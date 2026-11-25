package com.servando.homebudget.models.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddEventPurchaseDto(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive @DecimalMax("10000000") Double amount
) {
}
