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

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

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
        when(incomesRepository.findAll()).thenReturn(List.of(
                new IncomesModel("Primary", BillingCycle.MONTHLY, PayFrequency.BI_WEEKLY, 3669.21)
        ));
        when(billsRepository.findAll()).thenReturn(List.of(
                new BillsModel("Rent", 4522.55, BillCategory.RENT, BillingCycle.MONTHLY, 1)
        ));
        when(subscriptionsRepository.findAll()).thenReturn(List.of(
                new SubscriptionsModel("Music", BillingCycle.MONTHLY, 178.56, 1, PriorityLevel.IMPORTANT)
        ));
        when(creditCardsRepository.findAll()).thenReturn(List.of(
                new CreditCardsModel("CC", BillingCycle.MONTHLY, 0.0, 229.00, 1, 229.00)
        ));
        when(debtsRepository.findAll()).thenReturn(List.of());

        var result = dashboardService.getDashboard(5000.0, 100.0);

        assertEquals(7949.955000000001, result.getMonthlyIncome());
        assertEquals(4930.110000000001, result.getTotalObligations());
        assertEquals(2407.692307692308, result.getRecommendedTransfer());
        assertEquals(1372.4728402366866, result.getSpendablePerPaycheck());
        assertEquals(2973.691153846154, result.getSpendableMonthly());
        assertEquals(686.2364201183433, result.getSpendableWeekly());
    }

    @Test
    void getDashboard_returnsZeroSpendableWhenNoIncome() {
        when(incomesRepository.findAll()).thenReturn(List.of());
        when(billsRepository.findAll()).thenReturn(List.of());
        when(subscriptionsRepository.findAll()).thenReturn(List.of());
        when(creditCardsRepository.findAll()).thenReturn(List.of());
        when(debtsRepository.findAll()).thenReturn(List.of());

        var result = dashboardService.getDashboard(5000.0, 0.0);

        assertEquals(0.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(0.0, result.getSpendablePerPaycheck(), 1e-9);
        assertEquals(0.0, result.getSpendableMonthly(), 1e-9);
        assertEquals(0.0, result.getSpendableWeekly(), 1e-9);
    }

    @Test
    void getDashboard_marksOverBudgetAndUsesAllBillingCycleBranches() {
        when(incomesRepository.findAll()).thenReturn(List.of(
                new IncomesModel("Primary", BillingCycle.MONTHLY, PayFrequency.SEMI_MONTHLY, 1000.0)
        ));
        when(billsRepository.findAll()).thenReturn(List.of(
                new BillsModel("Annual", 1200.0, BillCategory.RENT, BillingCycle.ANNUAL, 1),
                new BillsModel("Semiannual", 600.0, BillCategory.RENT, BillingCycle.SEMIANNUAL, 1),
                new BillsModel("Optional", 240.0, BillCategory.RENT, BillingCycle.OPTIONAL, 1)
        ));
        when(subscriptionsRepository.findAll()).thenReturn(List.of());
        when(creditCardsRepository.findAll()).thenReturn(List.of(
                new CreditCardsModel("CC", BillingCycle.MONTHLY, 0.0, 0.0, 1, 0.0)
        ));
        var debt = new DebtsModel("Debt", BillingCycle.MONTHLY);
        debt.setAmountOwed(50.0);
        when(debtsRepository.findAll()).thenReturn(List.of(debt));

        var result = dashboardService.getDashboard(0.0, 200.0);

        assertEquals(220.0, result.getBillsMonthly(), 1e-9);
        assertEquals(0.0, result.getSubscriptionsMonthly(), 1e-9);
        assertEquals(0.0, result.getCreditCardsMonthly(), 1e-9);
        assertEquals(50.0, result.getTotalDebts(), 1e-9);
        assertEquals(PayFrequency.SEMI_MONTHLY, result.getFrequency());
        assertFalse(result.getIsOverBudget());
        assertEquals(200.0, result.getRecommendedTransfer(), 1e-9);
    }
}
