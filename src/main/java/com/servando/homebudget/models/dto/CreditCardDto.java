package com.servando.homebudget.models.dto;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("CreditCards")
public record CreditCardDto(
        @Id
        String id,
        @Indexed
        @Nonnull
        String name,
        Double currentBalance,
        Double minimumPayment,
        Double apr,
        Integer dueDay
) {
}
