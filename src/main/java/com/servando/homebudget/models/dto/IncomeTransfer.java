package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;

/**
 * One income's payday: what it moves to the bills account (the same share of every paycheck in the
 * household, plus the pay period buffer) and what that leaves of the paycheck to spend.
 */
public record IncomeTransfer(String incomeId, String name, PayFrequency frequency, Double perPaycheck,
                             Double spendablePerPaycheck) {
}
