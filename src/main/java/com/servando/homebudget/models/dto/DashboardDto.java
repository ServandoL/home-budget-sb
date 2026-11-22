package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class DashboardDto{
        @NotNull
        private Double targetMonthlyTransfer;
        @NotNull
        private Double monthlyIncome;
        @NotNull
        private Double billsMonthly;
        @NotNull
        private Double subscriptionsMonthly;
        @NotNull
        private Double creditCardsMonthly;
        @NotNull
        private Double totalObligations;
        /**
         * The transfer every paycheck makes, when they all make the same one (always so with one
         * income). Null without income, or when paychecks differ: then {@link #transfers} has each one's.
         */
        @Nullable
        private Double recommendedTransfer;
        /** What each income moves to the bills account each payday, and what that leaves of it. */
        @NotNull
        private List<IncomeTransfer> transfers;
        /** What's left of every paycheck, when it's the same for all of them; null like {@link #recommendedTransfer}. */
        @Nullable
        private Double spendablePerPaycheck;
        /**
         * How much a month with the fewest paydays draws from the bills account. 0 when two biweekly
         * (or four weekly) paychecks cover the bills; otherwise every payday moves the same amount and
         * the extra paydays make it up.
         */
        @NotNull
        private Double shortMonthShortfall;
        /**
         * The most the short months can draw from the bills account before an extra payday makes it
         * up: {@link #shortMonthShortfall} over the longest run of them (six for biweekly, three for
         * weekly). The cushion to keep there; 0 when the paydays cover the bills.
         */
        @NotNull
        private Double shortMonthsCushion;
        @NotNull
        private Double spendableWeekly;
        @NotNull
        private Double spendableMonthly;
        /** What's owed on debts, without credit cards. */
        @NotNull
        private Double totalDebts;
        /** What's carried on credit cards. */
        @NotNull
        private Double creditCardBalances;
        /** Debts and credit card balances together. */
        @NotNull
        private Double totalOwed;
        @NotNull
        private Integer billCounts;
        @NotNull
        private Integer subscriptionCounts;
        @NotNull
        private Integer creditCardCounts;
        @Nullable
        private PayFrequency frequency;
        @NotNull
        private Boolean isOverBudget;
        /** False when the target transfer is less than the monthly obligations. */
        @NotNull
        private Boolean transferCoversObligations;
        /** How much a month the target transfer falls short of the obligations; 0 when it covers them. */
        @NotNull
        private Double transferShortfall;


}
