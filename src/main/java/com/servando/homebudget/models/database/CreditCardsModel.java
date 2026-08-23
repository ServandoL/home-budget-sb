package com.servando.homebudget.models.database;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("CreditCards")
@Getter
@Setter
public class CreditCardsModel extends SharedProperties {
    @Nullable
    Double currentBalance;
    @Nullable
    Double minimumPayment;
    @Nullable
    Double apr;
    @Nullable
    Integer dueDay;

    public CreditCardsModel(String name, @Nullable Double apr, @Nullable Double currentBalance, @Nullable Integer dueDay, @Nullable Double minimumPayment) {
        super(name, Instant.now());
        this.apr = apr;
        this.currentBalance = currentBalance;
        this.dueDay = dueDay;
        this.minimumPayment = minimumPayment;
    }
}
