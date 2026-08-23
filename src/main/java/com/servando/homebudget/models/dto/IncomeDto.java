package com.servando.homebudget.models.dto;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Incomes")
public record IncomeDto(
        @Id
        String id,
        @Nonnull
        @Indexed
        String name,
        @Nonnull
        Double netAmount,
        @Nonnull
        PayFrequency frequency
) {
}
