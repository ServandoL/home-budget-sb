package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("CreditCards")
public record CreditCardsModel(
        @Id
        String id,
        @Nonnull
        @Indexed(unique = true)
        String name,
        Double currentBalance,
        Double minimumPayment,
        Double apr,
        Integer dueDay,
        Instant createdAt,
        Instant updatedAt
) {
}
