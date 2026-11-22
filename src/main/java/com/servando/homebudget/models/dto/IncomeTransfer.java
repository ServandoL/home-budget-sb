package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;

/**
 * One income's payday: what it moves to the bills account (the same share of every paycheck in the
 * household, plus the pay period buffer) and what that leaves of the paycheck to spend.
 *
 * @param extraPaydaysYearly paydays a year beyond the fewest a month brings: 2 for biweekly (a third
 *                           payday in two months), 4 for weekly (a fifth), none otherwise
 * @param extraPaydayTransfer what moves on one of those: just the buffer, the month's bills already
 *                            being paid, unless two paychecks can't cover the bills
 */
public record IncomeTransfer(String incomeId, String name, PayFrequency frequency, Double perPaycheck,
                             Double spendablePerPaycheck, Integer extraPaydaysYearly,
                             Double extraPaydayTransfer) {
}
