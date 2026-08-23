package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillingCycle;
import com.servando.homebudget.models.database.PayFrequency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateSubscriptionsRequestDto extends SharedRequestProperties {
        @NotNull
        Double amount;
        @Nullable
        BillingCycle billingCycle;
        @Nullable
        Integer billingDay;
}
