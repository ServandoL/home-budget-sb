package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.servando.homebudget.encryption.Encrypted;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

/**
 * A household's budget planning settings, shared by everyone in it. One per household:
 * MongoConfiguration creates a unique householdId index. The id is set here, not by MongoDB,
 * because the amounts are encrypted with the record's id as part of the key material.
 */
@Document("BudgetSettings")
@Getter
@Setter
public class BudgetSettingsModel {
    @Id
    private String id = new ObjectId().toHexString();

    @JsonIgnore
    @Field(targetType = FieldType.OBJECT_ID)
    private String householdId;

    /** The monthly amount to move to the bills account; null means the dashboard calculates it. */
    @Encrypted
    private Double targetMonthlyTransfer;

    /** Added to every payday transfer as a cushion. */
    @Encrypted
    private Double payPeriodBuffer;

    private Instant updatedAt;

    /** The email of the member who last saved them. */
    private String updatedBy;
}
