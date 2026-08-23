package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillingCycle;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SharedRequestProperties {
    @NotBlank
    @NotNull
    String name;
    @Nullable
    BillingCycle billingCycle;
}
