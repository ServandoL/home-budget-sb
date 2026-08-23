package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import com.servando.homebudget.models.database.PayFrequency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateIncomeRequestDto extends SharedRequestProperties {
        @NotNull
        Double netAmount;
        @Nullable
        PayFrequency frequency;
}
