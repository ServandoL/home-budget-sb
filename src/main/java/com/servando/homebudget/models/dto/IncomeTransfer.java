package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;

/**
 * What one income moves to the bills account each payday: the same share of every paycheck in the
 * household, plus the pay period buffer.
 */
public record IncomeTransfer(String incomeId, String name, PayFrequency frequency, Double perPaycheck) {
}
