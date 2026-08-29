package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CreateDebtDto extends SharedRequestProperties {
    @NotNull
    Double amount;
    @NotNull
    @NotBlank
    String from;
    @NotNull
    @NotBlank
    String description;
    @NotNull
    Instant debtStarted;
    @NotNull
    Double amountPaid;
    @Nullable
    Instant datePaid;
}
