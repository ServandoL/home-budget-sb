package com.servando.homebudget.models.database;

import com.servando.homebudget.encryption.Encrypted;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Getter
@Setter
public class SharedProperties {
    @Id
    String id;
    /**
     * The household that owns the record, stored as an ObjectId. Names are unique per household:
     * MongoConfiguration creates a unique (householdId, nameIndex) index on every collection.
     */
    @JsonIgnore
    @Field(targetType = FieldType.OBJECT_ID)
    String householdId;
    @Encrypted
    @NotBlank
    @NotNull
    String name;
    /**
     * A keyed hash of the name within the household, set by EncryptingMongoListener on every save.
     * The name is stored encrypted, so lookups by name and the unique index use this instead.
     */
    @JsonIgnore
    byte[] nameIndex;
    @NotNull
    BillingCycle billingCycle;
    @Nullable
    Instant createdAt;
    @Nullable
    Instant updatedAt;

    public SharedProperties(String name, BillingCycle billingCycle, Instant updatedAt) {
        var now = Instant.now();
        this.billingCycle = billingCycle;
        this.createdAt = now;
        this.name = name;
        this.updatedAt = updatedAt == null ? now : updatedAt;
        this.id = new ObjectId().toHexString();
    }
}
