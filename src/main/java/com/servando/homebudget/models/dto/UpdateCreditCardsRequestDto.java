package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import lombok.Getter;

@Getter
public class UpdateCreditCardsRequestDto extends SharedRequestProperties {

        @Nullable
        Double currentBalance;
        @Nullable
        Double minimumPayment;
        @Nullable
        Double apr;
        @Nullable
        Integer dueDay;
}
