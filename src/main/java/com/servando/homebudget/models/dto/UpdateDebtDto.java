package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UpdateDebtDto extends SharedRequestProperties {
    @Nullable
    String from;
    @Nullable
    String description;
    @Nullable
    String debtStarted;
    @Nullable
    Double amountPaid;
    @Nullable
    String datePaid;
    @Nullable
    Double amount;
}
