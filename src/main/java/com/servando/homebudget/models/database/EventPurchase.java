package com.servando.homebudget.models.database;

import java.time.Instant;

/** One thing bought at an event. Kept inside its event, whose purchases are stored encrypted. */
public record EventPurchase(String id, String name, Double amount, Instant addedAt) {
}
