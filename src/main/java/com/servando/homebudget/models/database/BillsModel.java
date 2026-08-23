package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Bills")
public record BillsModel(
        @Id
        @Nonnull
        String id,
        @Indexed(unique = true)
        @Nonnull
        String name,
        @Nonnull
        Double amount,
        int dueDay,
        @Nonnull
        BillCategory category,
        Instant createdAt,
        Instant updatedAt
) {
}
