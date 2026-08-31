package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.PayFrequency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class DashboardDto{
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
        @NotNull
        private Double recommendedTransfer;
        @NotNull
        private Double spendablePerPaycheck;
        @NotNull
        private Double spendableWeekly;
        @NotNull
        private Double spendableMonthly;
        @NotNull
        private Double totalDebts;
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


}
