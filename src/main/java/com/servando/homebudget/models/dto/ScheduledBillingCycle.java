package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillingCycle;
import jakarta.validation.constraints.AssertTrue;

/**
 * Bills, subscriptions and credit cards are paid on a schedule; only house repairs and debts can be
 * optional. The app offers only monthly, semiannual and annual for these.
 */
public interface ScheduledBillingCycle {
    BillingCycle getBillingCycle();

    @AssertTrue(message = "must be monthly, semiannual or annual")
    default boolean isBillingCycleScheduled() {
        return getBillingCycle() != BillingCycle.OPTIONAL;
    }
}
