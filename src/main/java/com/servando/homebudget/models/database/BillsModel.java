package com.servando.homebudget.models.database;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("Bills")
@Getter
@Setter
public class BillsModel extends SharedProperties {
    @NotNull
    Double amount;
    @Nullable
    Integer dueDay;
    @NotNull
    BillCategory category;

    public BillsModel(String name, Double amount, BillCategory category, @Nullable Integer dueDay) {
        super(name, Instant.now());
        this.amount = amount;
        this.category = category;
        this.dueDay = dueDay;
    }
}
