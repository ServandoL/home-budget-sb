package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CreateCreditCardsRequestDto extends SharedRequestProperties {
    @NotNull
    Double currentBalance;
    @Nullable
    Double minimumPayment;
    @Nullable
    Double apr;
    @Nullable
    Integer dueDay;
}
