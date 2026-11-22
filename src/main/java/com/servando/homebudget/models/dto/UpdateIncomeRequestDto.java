package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class UpdateIncomeRequestDto extends SharedRequestProperties {
    @Nullable
    @Positive
    Double netAmount;
    @Nullable
    PayFrequency frequency;
}
