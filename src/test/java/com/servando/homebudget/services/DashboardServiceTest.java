package com.servando.homebudget.services;

import com.servando.homebudget.models.database.*;
import com.servando.homebudget.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    private static final String HOUSEHOLD = "6afe67e6c0a8bf150c541825";

    @Mock
    private IncomesRepository incomesRepository;
    @Mock
    private BillsRepository billsRepository;
    @Mock
    private SubscriptionsRepository subscriptionsRepository;
    @Mock
    private CreditCardsRepository creditCardsRepository;
    @Mock
    private DebtsRepository debtsRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboard_calculatesAlignedSpendableValuesForBiWeeklyIncome() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new IncomesModel("Primary", BillingCycle.MONTHLY, PayFrequency.BI_WEEKLY, 3669.21)
        ));
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new BillsModel("Rent", 4522.55, BillCategory.RENT, BillingCycle.MONTHLY, 1)
        ));
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new SubscriptionsModel("Music", BillingCycle.MONTHLY, 178.56, 1, PriorityLevel.IMPORTANT)
        ));
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new CreditCardsModel("CC", BillingCycle.MONTHLY, 0.0, 229.00, 1, 229.00)
        ));
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 5000.0, 100.0);

        assertEquals(7949.955, result.getMonthlyIncome(), 1e-6);
        assertEquals(4930.11, result.getTotalObligations(), 1e-6);
        // 5000 a month over 26/12 paychecks a month, plus the $100 buffer on each.
        assertEquals(2407.692307692308, result.getRecommendedTransfer(), 1e-6);
        // The $100 buffer comes out of each of 26/12 paychecks a month: 216.67 a month.
        assertEquals(7949.955 - 4930.11 - 100.0 * 26 / 12, result.getSpendableMonthly(), 1e-6);
        assertEquals(2803.178333333333 * 12 / 26, result.getSpendablePerPaycheck(), 1e-6);
        assertEquals(2803.178333333333 * 12 / 52, result.getSpendableWeekly(), 1e-6);
    }

    @Test
    void getDashboard_countsMonthlyPayTwelveTimesAYear() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new IncomesModel("Salary", BillingCycle.MONTHLY, PayFrequency.MONTHLY, 4000.0)
        ));
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new BillsModel("Rent", 2000.0, BillCategory.RENT, BillingCycle.MONTHLY, 1)
        ));
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 1000.0, 100.0);

        // One paycheck a month: the month's figures are the paycheck's.
        assertEquals(4000.0, result.getMonthlyIncome(), 1e-9);
        assertEquals(1100.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(1900.0, result.getSpendableMonthly(), 1e-9);
        assertEquals(1900.0, result.getSpendablePerPaycheck(), 1e-9);
        assertEquals(PayFrequency.MONTHLY, result.getFrequency());
    }

    @Test
    void getDashboard_addsMonthlyPayToAnotherSchedule() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new IncomesModel("Salary", BillingCycle.MONTHLY, PayFrequency.MONTHLY, 3000.0),
                new IncomesModel("Wages", BillingCycle.MONTHLY, PayFrequency.BI_WEEKLY, 1200.0)
        ));
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 0.0, 0.0);

        // $3,000 once a month plus $1,200 26 times a year; "payday" is the more frequent schedule.
        assertEquals(3000.0 + 1200.0 * 26 / 12, result.getMonthlyIncome(), 1e-9);
        assertEquals(PayFrequency.BI_WEEKLY, result.getFrequency());
    }

    @Test
    void getDashboard_returnsZeroSpendableWhenNoIncome() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 5000.0, 0.0);

        assertEquals(0.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(0.0, result.getSpendablePerPaycheck(), 1e-9);
        assertEquals(0.0, result.getSpendableMonthly(), 1e-9);
        assertEquals(0.0, result.getSpendableWeekly(), 1e-9);
    }

    @Test
    void getDashboard_marksOverBudgetAndUsesAllBillingCycleBranches() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new IncomesModel("Primary", BillingCycle.MONTHLY, PayFrequency.SEMI_MONTHLY, 1000.0)
        ));
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new BillsModel("Annual", 1200.0, BillCategory.RENT, BillingCycle.ANNUAL, 1),
                new BillsModel("Semiannual", 600.0, BillCategory.RENT, BillingCycle.SEMIANNUAL, 1),
                new BillsModel("Optional", 240.0, BillCategory.RENT, BillingCycle.OPTIONAL, 1)
        ));
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new CreditCardsModel("CC", BillingCycle.MONTHLY, 0.0, 0.0, 1, 0.0)
        ));
        var debt = new DebtsModel("Debt", BillingCycle.MONTHLY);
        debt.setAmountOwed(50.0);
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(debt));

        var result = dashboardService.getDashboard(HOUSEHOLD, 0.0, 200.0);

        assertEquals(220.0, result.getBillsMonthly(), 1e-9);
        assertEquals(0.0, result.getSubscriptionsMonthly(), 1e-9);
        assertEquals(0.0, result.getCreditCardsMonthly(), 1e-9);
        assertEquals(50.0, result.getTotalDebts(), 1e-9);
        assertEquals(PayFrequency.SEMI_MONTHLY, result.getFrequency());
        assertFalse(result.getIsOverBudget());
        assertEquals(200.0, result.getRecommendedTransfer(), 1e-9);
        // 2000 a month in, 220 out, and the $200 buffer from each of 2 paychecks a month.
        assertEquals(1380.0, result.getSpendableMonthly(), 1e-9);
    }

    @Test
    void getDashboard_countsACardWithoutAMinimumAsNothingAndWorksOutWhatADebtOwes() {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of());
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(
                new CreditCardsModel("No minimum", BillingCycle.MONTHLY, null, 500.0, null, null),
                new CreditCardsModel("CC", BillingCycle.MONTHLY, 0.0, 229.00, 1, 25.0)
        ));
        // Saved before amountOwed was set on create: only the amount and what's been paid.
        var debt = new DebtsModel("Car", BillingCycle.MONTHLY);
        debt.setAmount(1000.0);
        debt.setAmountPaid(250.0);
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(List.of(debt));

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(25.0, result.getCreditCardsMonthly(), 1e-9);
        assertEquals(750.0, result.getTotalDebts(), 1e-9);
    }
}
