package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillingCycle;

public record MonthlyBillCost(Double amount, BillingCycle billingCycle) {
}
