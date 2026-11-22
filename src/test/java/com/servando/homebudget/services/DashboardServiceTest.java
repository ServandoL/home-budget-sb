package com.servando.homebudget.services;

import com.servando.homebudget.models.database.*;
import com.servando.homebudget.models.dto.DashboardDto;
import com.servando.homebudget.models.dto.IncomeTransfer;
import com.servando.homebudget.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    private static final String HOUSEHOLD = "6afe67e6c0a8bf150c541825";
    private static final double CENT = 0.005;

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

    // ── A renter paid every two weeks ────────────────────────────────────────────────────────────
    // $2,450 take-home biweekly. Rent 1,650, car payment 385, electric 140, phone 85, internet 70,
    // car insurance 1,080 every six months (180 a month). Netflix 15.49, Spotify 11.99, Prime 139 a
    // year (11.58 a month). A card with a $45 minimum. Bills 2,510, subscriptions 39.06, card 45.

    private static final double RENTER_PAY = 2450.0;
    private static final double RENTER_OBLIGATIONS = 2510.0 + (15.49 + 11.99 + 139.0 / 12) + 45.0;

    private void givenTheRenter(IncomesModel... incomes) {
        given(List.of(incomes),
                List.of(bill("Rent", 1650.0, BillingCycle.MONTHLY),
                        bill("Car payment", 385.0, BillingCycle.MONTHLY),
                        bill("Electric", 140.0, BillingCycle.MONTHLY),
                        bill("Phone", 85.0, BillingCycle.MONTHLY),
                        bill("Internet", 70.0, BillingCycle.MONTHLY),
                        bill("Car insurance", 1080.0, BillingCycle.SEMIANNUAL)),
                List.of(subscription("Netflix", 15.49, BillingCycle.MONTHLY),
                        subscription("Spotify", 11.99, BillingCycle.MONTHLY),
                        subscription("Prime", 139.0, BillingCycle.ANNUAL)),
                List.of(card("Visa", 45.0, 1240.50, BillingCycle.MONTHLY)),
                List.of());
    }

    @Test
    void getDashboard_sizesABiweeklyTransferSoATwoPaycheckMonthCoversTheBills() {
        givenTheRenter(income("Paycheck", PayFrequency.BI_WEEKLY, RENTER_PAY));

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 100.0);

        assertEquals(2594.06, result.getTotalObligations(), CENT);
        // Ten months a year bring two paychecks, so each one carries half the bills: 1,297.03 + $100.
        var transfer = result.getTransfers().get(0);
        assertEquals(RENTER_OBLIGATIONS / 2 + 100, transfer.perPaycheck(), 1e-9);
        assertEquals(1397.03, result.getRecommendedTransfer(), CENT);
        // What two paychecks move, less their buffers, pays the month's bills in full.
        assertEquals(result.getTotalObligations(), 2 * (transfer.perPaycheck() - 100), 1e-9);
        // Two months a year bring a third payday. The bills are already paid, so it moves just the buffer.
        assertEquals(2, transfer.extraPaydaysYearly());
        assertEquals(100.0, transfer.extraPaydayTransfer(), 1e-9);
        assertEquals(0.0, result.getShortMonthShortfall(), 1e-9);
        assertEquals(0.0, result.getShortMonthsCushion(), 1e-9);
        // What's left of each $2,450 paycheck. On average a month: the income less the bills and
        // the $100 buffer from each of 26 paydays, so the bills account grows only by the buffers.
        assertEquals(1052.97, transfer.spendablePerPaycheck(), CENT);
        assertEquals(transfer.spendablePerPaycheck(), result.getSpendablePerPaycheck(), 1e-9);
        assertEquals(RENTER_PAY * 26 / 12 - RENTER_OBLIGATIONS - 100.0 * 26 / 12, result.getSpendableMonthly(), 1e-9);
        assertEquals(2497.60, result.getSpendableMonthly(), CENT);
        assertEquals(result.getSpendableMonthly() * 12 / 52, result.getSpendableWeekly(), 1e-9);
        assertFalse(result.getIsOverBudget());
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_sizesAWeeklyTransferSoAFourPaycheckMonthCoversTheBills() {
        // $850 a week from a warehouse job, $1,400 rent and nothing else. Eight months bring four
        // paychecks and four bring five.
        given(List.of(income("Warehouse", PayFrequency.WEEKLY, 850.0)),
                List.of(bill("Rent", 1400.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(350.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(1400.0, 4 * result.getTransfers().get(0).perPaycheck(), 1e-9);
        // The fifth Fridays move nothing: 48 transfers of 350 is the year's 16,800 of rent exactly.
        assertEquals(4, result.getTransfers().get(0).extraPaydaysYearly());
        assertEquals(0.0, result.getTransfers().get(0).extraPaydayTransfer(), 1e-9);
        assertEquals(850.0 * 52 / 12 - 1400, result.getSpendableMonthly(), 1e-9);
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_leavesAMonthlyPaycheckAsItIs() {
        // Paid once a month, every month is the same: nothing extra builds up.
        given(List.of(income("Salary", PayFrequency.MONTHLY, 4000.0)),
                List.of(bill("Rent", 2000.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 1000.0, 100.0);

        assertEquals(4000.0, result.getMonthlyIncome(), 1e-9);
        // The bills are more than the $1,000 target, so the transfer pays the bills.
        assertEquals(2100.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(1900.0, result.getSpendableMonthly(), 1e-9);
        assertEquals(1900.0, result.getSpendablePerPaycheck(), 1e-9);
        assertEquals(0, result.getTransfers().get(0).extraPaydaysYearly());
        assertEquals(PayFrequency.MONTHLY, result.getFrequency());
    }

    @Test
    void getDashboard_givesEachIncomeItsOwnTransferAndWhatItLeaves() {
        // A biweekly $2,000 and a monthly $3,000. The leanest month brings 2 × 2,000 + 3,000 = 7,000,
        // and the $2,285.99 of bills is 32.66% of it.
        var wages = income("Wages", PayFrequency.BI_WEEKLY, 2000.0);
        var salary = income("Salary", PayFrequency.MONTHLY, 3000.0);
        given(List.of(wages, salary),
                List.of(bill("Rent", 1800.0, BillingCycle.MONTHLY),
                        bill("Car insurance", 1200.0, BillingCycle.SEMIANNUAL),
                        bill("Property tax", 2400.0, BillingCycle.ANNUAL)),
                List.of(subscription("Netflix", 15.99, BillingCycle.MONTHLY),
                        subscription("iCloud", 120.0, BillingCycle.ANNUAL)),
                List.of(card("Visa", 60.0, 900.0, BillingCycle.MONTHLY)),
                List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 100.0);

        assertEquals(2285.99, result.getTotalObligations(), 1e-9);
        var share = 2285.99 / 7000;
        IncomeTransfer wagesTransfer = result.getTransfers().get(0);
        IncomeTransfer salaryTransfer = result.getTransfers().get(1);
        assertEquals(wages.getId(), wagesTransfer.incomeId());
        assertEquals(2000 * share + 100, wagesTransfer.perPaycheck(), 1e-9);
        assertEquals(753.14, wagesTransfer.perPaycheck(), CENT);
        assertEquals(1079.71, salaryTransfer.perPaycheck(), CENT);
        // A month with two biweekly paychecks covers the bills.
        assertEquals(2285.99, 2 * (wagesTransfer.perPaycheck() - 100) + (salaryTransfer.perPaycheck() - 100), 1e-9);
        // Each paycheck keeps its own amount; no one figure fits both, so the household's is null.
        assertEquals(2000 - wagesTransfer.perPaycheck(), wagesTransfer.spendablePerPaycheck(), 1e-9);
        assertEquals(1920.29, salaryTransfer.spendablePerPaycheck(), CENT);
        assertNull(result.getRecommendedTransfer());
        assertNull(result.getSpendablePerPaycheck());
        // Only the biweekly income has extra paydays; the monthly one never does.
        assertEquals(2, wagesTransfer.extraPaydaysYearly());
        assertEquals(0, salaryTransfer.extraPaydaysYearly());
        assertEquals(7333.33 - 2285.99 - 100.0 * 38 / 12, result.getSpendableMonthly(), 0.01);
        assertEquals(PayFrequency.BI_WEEKLY, result.getFrequency());
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_givesTwoMatchingPaychecksOneFigure() {
        // Two partners, each $2,000 every two weeks, $4,000 rent: each paycheck moves $1,000 + $100.
        given(List.of(income("Sam", PayFrequency.BI_WEEKLY, 2000.0), income("Kim", PayFrequency.BI_WEEKLY, 2000.0)),
                List.of(bill("Rent", 4000.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 100.0);

        assertEquals(1100.0, result.getTransfers().get(0).perPaycheck(), 1e-9);
        assertEquals(1100.0, result.getTransfers().get(1).perPaycheck(), 1e-9);
        assertEquals(1100.0, result.getRecommendedTransfer(), 1e-9);
        assertEquals(900.0, result.getSpendablePerPaycheck(), 1e-9);
        // 8,666.67 a month in, the 4,000 of rent, and the buffer from all 52 paydays a year.
        assertEquals(2 * 2000.0 * 26 / 12 - 4000 - 100.0 * 52 / 12, result.getSpendableMonthly(), 1e-9);
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_movesTheBillsEvenWhenTheTargetIsLess() {
        givenTheRenter(income("Paycheck", PayFrequency.BI_WEEKLY, RENTER_PAY));

        var result = dashboardService.getDashboard(HOUSEHOLD, 2000.0, 100.0);

        // The bills account still gets the 2,594.06 the bills need, not the 2,000 typed.
        assertEquals(RENTER_OBLIGATIONS / 2 + 100, result.getTransfers().get(0).perPaycheck(), 1e-9);
        // The target stays what the household set, and the gap is still reported.
        assertEquals(2000.0, result.getTargetMonthlyTransfer(), 1e-9);
        assertFalse(result.getTransferCoversObligations());
        assertEquals(594.06, result.getTransferShortfall(), CENT);
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_movesALargerTargetInFull() {
        givenTheRenter(income("Paycheck", PayFrequency.BI_WEEKLY, RENTER_PAY));

        var result = dashboardService.getDashboard(HOUSEHOLD, 3000.0, 100.0);

        assertEquals(1600.0, result.getRecommendedTransfer(), 1e-9);
        // Exactly the 3,000 target each month, and the buffer from every payday.
        assertEquals(RENTER_PAY * 26 / 12 - 3000 - 100.0 * 26 / 12, result.getSpendableMonthly(), 1e-9);
        assertTrue(result.getTransferCoversObligations());
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_spreadsTheBillsOverEveryPaydayWhenTwoPaychecksCantCoverThem() {
        // $1,000 every two weeks against $2,100 of bills. Two paychecks bring 2,000, less than the
        // bills, but 26 a year bring 2,166.67 a month on average: the third paydays are needed.
        given(List.of(income("Paycheck", PayFrequency.BI_WEEKLY, 1000.0)),
                List.of(bill("Rent", 1800.0, BillingCycle.MONTHLY), bill("Electric", 300.0, BillingCycle.MONTHLY)),
                List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        // 2,100 × 12 ÷ 26 on every payday, the third ones too.
        var transfer = result.getTransfers().get(0);
        assertEquals(2100.0 * 12 / 26, transfer.perPaycheck(), 1e-9);
        assertEquals(969.23, transfer.perPaycheck(), CENT);
        assertEquals(transfer.perPaycheck(), transfer.extraPaydayTransfer(), 1e-9);
        // A two-payday month moves 1,938.46 and draws the other 161.54 from the bills account; the
        // two third paydays put it back.
        assertEquals(161.54, result.getShortMonthShortfall(), CENT);
        // Biweekly pay can bring six two-payday months in a row, so the account needs 6 × 161.54 on hand.
        assertEquals(969.23, result.getShortMonthsCushion(), CENT);
        assertEquals(2166.67 - 2100, result.getSpendableMonthly(), 0.01);
        assertFalse(result.getIsOverBudget());
        assertMovedAndLeftOverAreTheIncome(result);
    }

    @Test
    void getDashboard_givesNoPaycheckFiguresWithoutIncome() {
        // Between jobs: the rent is still due and there's no paycheck to move it from.
        given(List.of(), List.of(bill("Rent", 1650.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 2000.0, 100.0);

        assertEquals(-1650.0, result.getSpendableMonthly(), 1e-9);
        assertEquals(-1650.0 * 12 / 52, result.getSpendableWeekly(), 1e-9);
        assertNull(result.getSpendablePerPaycheck());
        assertNull(result.getRecommendedTransfer());
        assertTrue(result.getTransfers().isEmpty());
        assertEquals(0.0, result.getShortMonthShortfall(), 1e-9);
        assertTrue(result.getIsOverBudget());
    }

    @Test
    void getDashboard_isEmptyForANewHousehold() {
        given(List.of(), List.of(), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 100.0);

        assertEquals(0.0, result.getSpendableMonthly(), 1e-9);
        assertNull(result.getSpendablePerPaycheck());
        assertFalse(result.getIsOverBudget());
    }

    @Test
    void getDashboard_isNotOverBudgetWhenTheIncomeExactlyPaysTheBills() {
        // 4522.55 + 178.56 + 229.00 adds up to 4930.110000000001, a hair over the 4930.11 paid.
        given(List.of(income("Salary", PayFrequency.MONTHLY, 4930.11)),
                List.of(bill("Rent", 4522.55, BillingCycle.MONTHLY)),
                List.of(subscription("Music", 178.56, BillingCycle.MONTHLY)),
                List.of(card("Visa", 229.00, 0.0, BillingCycle.MONTHLY)),
                List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(0.0, result.getSpendableMonthly(), 1e-6);
        assertFalse(result.getIsOverBudget());
    }

    @Test
    void getDashboard_isOverBudgetByACent() {
        given(List.of(income("Salary", PayFrequency.MONTHLY, 1499.99)),
                List.of(bill("Rent", 1500.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(-0.01, result.getSpendableMonthly(), 1e-9);
        assertTrue(result.getIsOverBudget());
    }

    @Test
    void getDashboard_takesATargetTypedToMatchTheObligationsAsCoveringThem() {
        given(List.of(income("Paycheck", PayFrequency.BI_WEEKLY, 3669.21)),
                List.of(bill("Rent", 4522.55, BillingCycle.MONTHLY)),
                List.of(subscription("Music", 178.56, BillingCycle.MONTHLY)),
                List.of(card("Visa", 229.00, 0.0, BillingCycle.MONTHLY)),
                List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 4930.11, 100.0);

        assertTrue(result.getTotalObligations() > 4930.11);
        assertTrue(result.getTransferCoversObligations());
        assertEquals(0.0, result.getTransferShortfall());
    }

    @Test
    void getDashboard_stillReportsAShortfallOfACent() {
        given(List.of(income("Paycheck", PayFrequency.BI_WEEKLY, 3000.0)),
                List.of(bill("Rent", 1500.0, BillingCycle.MONTHLY)), List.of(), List.of(), List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, 1499.99, 0.0);

        assertFalse(result.getTransferCoversObligations());
        assertEquals(0.01, result.getTransferShortfall(), 1e-9);
    }

    @Test
    void getDashboard_countsACardMinimumEveryMonthWhateverItsBillingCycle() {
        // Minimum payments are due monthly; a card saved as annual still owes $35 every month.
        given(List.of(), List.of(), List.of(),
                List.of(card("Store card", 35.0, 410.0, BillingCycle.ANNUAL),
                        card("Visa", 25.0, 500.0, BillingCycle.SEMIANNUAL),
                        card("No minimum", null, 500.0, BillingCycle.MONTHLY)),
                List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(60.0, result.getCreditCardsMonthly(), 1e-9);
    }

    @Test
    void getDashboard_spreadsBillsAndSubscriptionsOverTheirBillingCycle() {
        given(List.of(),
                List.of(bill("Property tax", 2400.0, BillingCycle.ANNUAL),
                        bill("Car insurance", 1080.0, BillingCycle.SEMIANNUAL),
                        bill("Rent", 1650.0, BillingCycle.MONTHLY),
                        bill("Water", 45.0, null)),
                List.of(subscription("Prime", 139.0, BillingCycle.ANNUAL),
                        subscription("Netflix", 15.49, null)),
                List.of(),
                List.of());

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        // 200 + 180 + 1,650 + 45; a record saved without a cycle counts as monthly.
        assertEquals(2075.0, result.getBillsMonthly(), 1e-9);
        assertEquals(139.0 / 12 + 15.49, result.getSubscriptionsMonthly(), 1e-9);
    }

    @Test
    void getDashboard_totalsWhatsOwedOnDebtsAndCards() {
        // A car loan of 12,000 with 4,350 paid, saved before amountOwed was stored; a family loan of
        // 750 owed. Two cards carrying 1,240.50 and 300.
        var carLoan = new DebtsModel("Car loan", BillingCycle.MONTHLY);
        carLoan.setAmount(12000.0);
        carLoan.setAmountPaid(4350.0);
        var familyLoan = new DebtsModel("Mom", BillingCycle.OPTIONAL);
        familyLoan.setAmountOwed(750.0);
        given(List.of(), List.of(), List.of(),
                List.of(card("Visa", 45.0, 1240.50, BillingCycle.MONTHLY),
                        card("Store card", 25.0, 300.0, BillingCycle.MONTHLY),
                        card("Paid off", null, null, BillingCycle.MONTHLY)),
                List.of(carLoan, familyLoan));

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 0.0);

        assertEquals(8400.0, result.getTotalDebts(), 1e-9);
        assertEquals(1540.50, result.getCreditCardBalances(), 1e-9);
        assertEquals(9940.50, result.getTotalOwed(), 1e-9);
    }

    @Test
    void getDashboard_ignoresAnIncomeThatPaysNothing() {
        // A side gig saved at $0, and an old record at -$200, from before incomes had to be positive.
        givenTheRenter(income("Paycheck", PayFrequency.BI_WEEKLY, RENTER_PAY),
                income("Side gig", PayFrequency.WEEKLY, 0.0),
                income("Old record", PayFrequency.MONTHLY, -200.0));

        var result = dashboardService.getDashboard(HOUSEHOLD, null, 100.0);

        assertEquals(RENTER_PAY * 26 / 12, result.getMonthlyIncome(), 1e-9);
        assertEquals(1, result.getTransfers().size());
        assertEquals("Paycheck", result.getTransfers().get(0).name());
        assertEquals(RENTER_OBLIGATIONS / 2 + 100, result.getRecommendedTransfer(), 1e-9);
        assertEquals(PayFrequency.BI_WEEKLY, result.getFrequency());
        assertMovedAndLeftOverAreTheIncome(result);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────

    private void given(List<IncomesModel> incomes, List<BillsModel> bills, List<SubscriptionsModel> subscriptions,
                       List<CreditCardsModel> cards, List<DebtsModel> debts) {
        when(incomesRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(incomes);
        when(billsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(bills);
        when(subscriptionsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(subscriptions);
        when(creditCardsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(cards);
        when(debtsRepository.findAllByHouseholdId(HOUSEHOLD, Sort.unsorted())).thenReturn(debts);
    }

    private static IncomesModel income(String name, PayFrequency frequency, double net) {
        return new IncomesModel(name, BillingCycle.MONTHLY, frequency, net);
    }

    private static BillsModel bill(String name, double amount, BillingCycle cycle) {
        return new BillsModel(name, amount, BillCategory.RENT, cycle, 1);
    }

    private static SubscriptionsModel subscription(String name, double amount, BillingCycle cycle) {
        return new SubscriptionsModel(name, cycle, amount, 1, PriorityLevel.IMPORTANT);
    }

    private static CreditCardsModel card(String name, Double minimum, Double balance, BillingCycle cycle) {
        return new CreditCardsModel(name, cycle, 24.99, balance, 1, minimum);
    }

    /**
     * Everything moved to the bills account in a year (regular paydays and extra ones), plus what's
     * left over, is the income.
     */
    private static void assertMovedAndLeftOverAreTheIncome(DashboardDto result) {
        double moved = 0;
        double kept = 0;
        for (var t : result.getTransfers()) {
            var regular = paychecksPerYear(t.frequency()) - t.extraPaydaysYearly();
            var net = t.perPaycheck() + t.spendablePerPaycheck();
            moved += t.perPaycheck() * regular + t.extraPaydayTransfer() * t.extraPaydaysYearly();
            kept += t.spendablePerPaycheck() * regular + (net - t.extraPaydayTransfer()) * t.extraPaydaysYearly();
        }
        assertEquals(result.getMonthlyIncome(), (moved + kept) / 12, 1e-6);
        assertEquals(result.getSpendableMonthly(), kept / 12, 1e-6);
    }

    private static double paychecksPerYear(PayFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case SEMI_MONTHLY -> 24;
            case MONTHLY -> 12;
        };
    }
}
