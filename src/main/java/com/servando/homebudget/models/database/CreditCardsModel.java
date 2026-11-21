package com.servando.homebudget.models.database;

import com.servando.homebudget.encryption.Encrypted;
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
    @Encrypted
    @Nullable
    Double currentBalance;
    @Encrypted
    @Nullable
    Double minimumPayment;
    @Encrypted
    @Nullable
    Double apr;
    @Nullable
    Integer dueDay;

    public CreditCardsModel(String name, BillingCycle billingCycle, @Nullable Double apr, @Nullable Double currentBalance, @Nullable Integer dueDay, @Nullable Double minimumPayment) {
        super(name, billingCycle, Instant.now());
        this.apr = apr;
        this.currentBalance = currentBalance;
        this.dueDay = dueDay;
        this.minimumPayment = minimumPayment;
    }
}
