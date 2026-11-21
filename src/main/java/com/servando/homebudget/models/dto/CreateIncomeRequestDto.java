package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import com.servando.homebudget.models.database.PayFrequency;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateIncomeRequestDto extends SharedRequestProperties {
        @NotNull
        Double netAmount;
        /** Required: the dashboard can't count an income without knowing how often it's paid. */
        @NotNull
        PayFrequency frequency;
}
