package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class CreateEventBudgetDto extends SharedRequestProperties {
    @NotNull
    @Positive
    @DecimalMax("10000000")
    Double budget;
    @Nullable
    @Pattern(regexp = "(\\d{4}/\\d{2}/\\d{2})?")
    String date;
}
