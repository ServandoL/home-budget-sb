package com.servando.homebudget.models.database;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Getter
@Setter
public class SharedProperties {
    @Id
    String id;
    @NotBlank
    @NotNull
    @Indexed(unique = true)
    String name;
    @Nullable
    Instant createdAt;
    @Nullable
    Instant updatedAt;

    public SharedProperties(String name, Instant updatedAt) {
        var now = Instant.now();
        this.createdAt = now;
        this.name = name;
        this.updatedAt = updatedAt == null ? now : updatedAt;
        this.id = new ObjectId().toHexString();
    }
}
