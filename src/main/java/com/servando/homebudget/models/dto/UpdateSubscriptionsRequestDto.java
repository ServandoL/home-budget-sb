package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import com.servando.homebudget.models.database.BillingCycle;
import com.servando.homebudget.models.database.PriorityLevel;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateSubscriptionsRequestDto extends SharedRequestProperties {
    @Nullable
    Double amount;
    @Nullable
    Integer billingDay;
    @Nullable
    PriorityLevel priorityLevel;
}
