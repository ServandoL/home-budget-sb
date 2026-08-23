package com.servando.homebudget.models.dto;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Bills")
public record BillDto(
        @Id
        @Nonnull
        String id,
        @Indexed
        @Nonnull
        String name,
        @Nonnull
        Double amount,
        int dueDay,
        @Nonnull
        BillCategory category
) {
}
