package com.servando.homebudget.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.servando.homebudget.encryption.Encrypted;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A spending limit for one outing and what was bought against it. Temporary: it isn't part of the
 * monthly budget, and the household deletes it once it's done.
 */
@Document("EventBudgets")
@Getter
@Setter
public class EventBudgetModel extends SharedProperties {
    @Encrypted
    @NotNull
    Double budget;
    /** The day of the event, YYYY/MM/DD, as the app stores dates, if one was given. */
    @Nullable
    String date;
    /** When it was marked done; null while it's in progress. */
    @Nullable
    Instant doneAt;
    /** Newest first. The list is encrypted as one value, so it's read and written whole. */
    @Encrypted
    List<EventPurchase> purchases = new ArrayList<>();
    /**
     * Two people adding purchases at once each rewrite the whole list; the version check makes the
     * second save fail and retry on the newer list instead of dropping the first one's purchase.
     */
    @Version
    @JsonIgnore
    Long version;

    public EventBudgetModel(String name, Instant updatedAt) {
        super(name, BillingCycle.OPTIONAL, updatedAt);
    }

    public double getSpent() {
        return purchases == null ? 0 : purchases.stream().mapToDouble(p -> p.amount() == null ? 0 : p.amount()).sum();
    }
}
