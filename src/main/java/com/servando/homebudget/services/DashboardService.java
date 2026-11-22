package com.servando.homebudget.services;

import com.servando.homebudget.models.database.*;
import com.servando.homebudget.models.dto.DashboardDto;
import com.servando.homebudget.models.dto.IncomeTransfer;
import com.servando.homebudget.models.dto.MonthlyBillCost;
import com.servando.homebudget.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DashboardService {
    @Autowired
    private IncomesRepository incomesRepository;
    @Autowired
    private BillsRepository billsRepository;
    @Autowired
    private SubscriptionsRepository subscriptionsRepository;
    @Autowired
    private CreditCardsRepository creditCardsRepository;
    @Autowired
    private DebtsRepository debtsRepository;
    ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Double MONTHS_PER_YEAR = 12.0;
    private final Double WEEKS_PER_YEAR = 52.0;
    private static final double HALF_A_CENT = 0.005;


    /**
     * Builds a dashboard snapshot by loading all financial records in parallel and deriving
     * monthly, weekly, and per-paycheck budget values from them.
     *
     * @param householdId the household whose records to total
     * @param targetMonthlyTransfer the amount the household moves into its bills account each month;
     *                              when null, it's the monthly obligations
     * @param payPeriodBuffer additional buffer amount to reserve from each pay period
     * @return a populated dashboard summary
     */
    public DashboardDto getDashboard(String householdId, Double targetMonthlyTransfer, Double payPeriodBuffer) {
        var dashboard = new DashboardDto();
        CompletableFuture<List<IncomesModel>> incomesFuture =
                CompletableFuture.supplyAsync(() -> incomesRepository.findAllByHouseholdId(householdId, Sort.unsorted()), executor);
        CompletableFuture<List<BillsModel>> billsFuture =
                CompletableFuture.supplyAsync(() -> billsRepository.findAllByHouseholdId(householdId, Sort.unsorted()), executor);
        CompletableFuture<List<SubscriptionsModel>> subsFuture =
                CompletableFuture.supplyAsync(() -> subscriptionsRepository.findAllByHouseholdId(householdId, Sort.unsorted()), executor);
        CompletableFuture<List<CreditCardsModel>> cardsFuture =
                CompletableFuture.supplyAsync(() -> creditCardsRepository.findAllByHouseholdId(householdId, Sort.unsorted()), executor);
        CompletableFuture<List<DebtsModel>> debtsFuture =
                CompletableFuture.supplyAsync(() -> debtsRepository.findAllByHouseholdId(householdId, Sort.unsorted()), executor);
        CompletableFuture.allOf(
                incomesFuture, billsFuture, subsFuture, cardsFuture, debtsFuture
        ).join();

        List<IncomesModel> incomes = incomesFuture.join();
        List<BillsModel> bills = billsFuture.join();
        List<SubscriptionsModel> subscriptions = subsFuture.join();
        List<CreditCardsModel> creditCards = cardsFuture.join();
        List<DebtsModel> debts = debtsFuture.join();

        var billsMonthly = getCycleCost(
                bills.stream().map(e -> new MonthlyBillCost(e.getAmount(), e.getBillingCycle())).toList()
        );
        var subscriptionsMonthly = getCycleCost(
                subscriptions.stream().map(e -> new MonthlyBillCost(e.getAmount(), e.getBillingCycle())).toList()
        );
        var ccMonthly = getCycleCost(
                creditCards.stream().map(e -> new MonthlyBillCost(e.getMinimumPayment(), e.getBillingCycle())).toList()
        );
        var incomeFrequency = getIncomesFrequency(incomes);
        var totalObligations = billsMonthly + subscriptionsMonthly + ccMonthly;
        var totalIncome = calculateMonthly(incomes);
        var effectiveTargetMonthlyTransfer = targetMonthlyTransfer != null
                ? targetMonthlyTransfer
                : getAutoTargetMonthlyTransfer(totalObligations);
        var transfers = getTransfers(incomes, totalIncome, effectiveTargetMonthlyTransfer, payPeriodBuffer);
        var recommendedTransferPerPayPeriod = getRecommendedTransferPerPayPeriod(incomes, effectiveTargetMonthlyTransfer, payPeriodBuffer);
        // What leaves the main account each month is the transfer, which is the target when it's
        // larger than the obligations; a smaller target doesn't free money, the bills still need paying.
        // Without income there's no paycheck to transfer from, only the bills.
        var committedMonthly = incomes.isEmpty()
                ? totalObligations
                : Math.max(effectiveTargetMonthlyTransfer, totalObligations);
        var transferShortfall = getTransferShortfall(totalObligations, effectiveTargetMonthlyTransfer);
        var spendableMonthly = getSpendableMonthly(totalIncome, committedMonthly, payPeriodBuffer, incomes);
        var spendablePerPaycheck = getSpendablePerPaycheckFromMonthly(spendableMonthly, incomes);
        var spendableWeekly = getSpendableWeeklyFromMonthly(spendableMonthly);
        dashboard.setMonthlyIncome(totalIncome);
        dashboard.setTargetMonthlyTransfer(effectiveTargetMonthlyTransfer);
        dashboard.setTransferShortfall(transferShortfall);
        dashboard.setTransferCoversObligations(transferShortfall == 0.0);
        dashboard.setBillsMonthly(billsMonthly);
        dashboard.setSubscriptionsMonthly(subscriptionsMonthly);
        dashboard.setCreditCardsMonthly(ccMonthly);
        dashboard.setTotalObligations(totalObligations);
        dashboard.setRecommendedTransfer(recommendedTransferPerPayPeriod);
        dashboard.setTransfers(transfers);
        dashboard.setSpendablePerPaycheck(spendablePerPaycheck);
        dashboard.setSpendableMonthly(spendableMonthly);
        dashboard.setSpendableWeekly(spendableWeekly);
        dashboard.setIsOverBudget(dashboard.getSpendableMonthly() < (double) 0);
        dashboard.setTotalDebts(getTotalDebt(debts));
        dashboard.setSubscriptionCounts(subscriptions.size());
        dashboard.setCreditCardCounts(creditCards.size());
        dashboard.setBillCounts(bills.size());
        dashboard.setFrequency(incomeFrequency);
        return dashboard;
    }

    /**
     * Auto-calculates a safe monthly transfer target when the caller doesn't supply one: the amount
     * needed to cover total monthly obligations (bills, subscriptions, credit card minimums).
     *
     * @param totalObligations total monthly obligations
     * @return auto-calculated safe monthly transfer amount
     */
    private Double getAutoTargetMonthlyTransfer(Double totalObligations) {
        return totalObligations;
    }

    /**
     * How much a month the target falls short of the obligations. The obligations are a sum of
     * decimals (4522.55 + 178.56 + 229.00 is 4930.110000000001), so a target typed to match them
     * counts as covering them: anything under half a cent is no shortfall.
     *
     * @param totalObligations total monthly obligations
     * @param targetMonthlyTransfer what the household moves to the bills account each month
     * @return the shortfall, or 0 when the target covers the obligations
     */
    private Double getTransferShortfall(Double totalObligations, Double targetMonthlyTransfer) {
        var shortfall = totalObligations - targetMonthlyTransfer;
        return shortfall < HALF_A_CENT ? 0.0 : shortfall;
    }

    /**
     * The transfer for each income. Every paycheck moves the same share of itself, the target over the
     * household's monthly income, so incomes on different schedules or of different sizes each pay
     * their part. Each paycheck also moves the buffer.
     *
     * @param incomes income records
     * @param monthlyIncome the household's total monthly income
     * @param targetMonthlyTransfer what the household moves to the bills account each month
     * @param buffer extra amount moved on every paycheck
     * @return one transfer per income, in the order given
     */
    private List<IncomeTransfer> getTransfers(List<IncomesModel> incomes, Double monthlyIncome,
                                              Double targetMonthlyTransfer, Double buffer) {
        var share = monthlyIncome > 0 ? targetMonthlyTransfer / monthlyIncome : 0.0;
        var perPaycheckBuffer = buffer == null ? 0.0 : buffer;
        return incomes.stream()
                .map(income -> new IncomeTransfer(income.getId(), income.getName(), income.getFrequency(),
                        income.getNetAmount() * share + perPaycheckBuffer))
                .toList();
    }

    /**
     * The transfer per paycheck spread evenly over every paycheck in the household. With one income
     * it's that income's transfer; with several, {@link #getTransfers} gives each one its own.
     *
     * @param incomes income records used to count paychecks
     * @param targetMonthlyTransfer desired monthly transfer target
     * @param buffer extra amount to add per pay period
     * @return transfer per paycheck, on average
     */
    private Double getRecommendedTransferPerPayPeriod(List<IncomesModel> incomes, Double targetMonthlyTransfer, Double buffer) {
        if (incomes.isEmpty()) return (double) 0;
        var paychecksPerMonth = getTotalPayPeriodsPerYear(incomes) / MONTHS_PER_YEAR;
        return (targetMonthlyTransfer / paychecksPerMonth) + (buffer == null ? 0.0 : buffer);
    }

    /**
     * Calculates how much money remains in the main account each month after the transfer to the
     * bills account and any configured buffer.
     *
     * @param monthlyIncome total monthly income
     * @param committedMonthly what's moved out each month: the larger of the target transfer and the obligations
     * @param buffer optional pay-period buffer to subtract from the monthly spendable amount
     * @param incomes income records, whose paychecks each move the buffer
     * @return spendable monthly amount
     */
    private Double getSpendableMonthly(Double monthlyIncome, Double committedMonthly, Double buffer, List<IncomesModel> incomes) {
        if (buffer != null && buffer > 0 && !incomes.isEmpty()) {
            // Every paycheck in the household moves the buffer, so a month holds that many of them.
            var paychecksPerMonth = getTotalPayPeriodsPerYear(incomes) / MONTHS_PER_YEAR;
            return (monthlyIncome - committedMonthly) - (buffer * paychecksPerMonth);
        }
        return monthlyIncome - committedMonthly;
    }

    /**
     * Sums all incomes after converting each one to a monthly equivalent.
     *
     * @param incomes income records to normalize
     * @return total monthly income
     */
    private Double calculateMonthly(List<IncomesModel> incomes) {
        if (incomes.isEmpty()) return (double) 0;
        return incomes.stream().mapToDouble(this::getMonthlyIncome).sum();
    }

    /**
     * Converts a single income record into an annual amount.
     *
     * @param income income record to normalize
     * @return annualized income
     */
    private Double getAnnualIncome(IncomesModel income) {
        var frequency = getFrequency(income.getFrequency());
        return income.getNetAmount() * frequency;
    }

    /**
     * Converts a single income record into a monthly amount.
     *
     * @param income income record to normalize
     * @return monthly income
     */
    private Double getMonthlyIncome(IncomesModel income) {
        return getAnnualIncome(income) / MONTHS_PER_YEAR;
    }

    /**
     * Maps a pay frequency to the number of pay periods per year.
     *
     * @param freq pay frequency to convert
     * @return annual pay periods for the frequency
     */
    private Double getFrequency(PayFrequency freq) {
        double biweeklyFrequency = 26.0;
        double semimonthlyFrequency = 24.0;
        // Every value named, no default: a new frequency must be counted here, not as twice a month.
        return switch (freq) {
            case WEEKLY -> WEEKS_PER_YEAR;
            case BI_WEEKLY -> biweeklyFrequency;
            case SEMI_MONTHLY -> semimonthlyFrequency;
            case MONTHLY -> MONTHS_PER_YEAR;
        };
    }

    /**
     * (monthlyIncome − obligations) × 12 ÷ 52
     */
    private Double getSpendableWeeklyFromMonthly(Double spendableMonthly) {
        return (spendableMonthly * MONTHS_PER_YEAR) / WEEKS_PER_YEAR;
    }

    /**
     * Normalizes a billing cycle to the number of months it spans.
     *
     * @param billingCycle billing cycle to convert; none counts as monthly, as the UI shows it
     * @return month multiplier for the cycle
     */
    private Double getMonthlyBillingCycle(BillingCycle billingCycle) {
        if (billingCycle == null) return 1.0;
        return switch (billingCycle) {
            case ANNUAL, OPTIONAL -> MONTHS_PER_YEAR;
            case SEMIANNUAL -> 6.0;
            default -> 1.0;
        };
    }

    /**
     * Chooses the highest pay frequency present so spendable values can be normalized safely.
     *
     * @param incomes income records to inspect
     * @return the highest observed pay frequency, or bi-weekly when no income exists
     */
    private PayFrequency getIncomesFrequency(List<IncomesModel> incomes) {
        if (incomes.isEmpty()) return PayFrequency.BI_WEEKLY;
        return incomes.stream().max(Comparator.comparing(e -> getFrequency(e.getFrequency()))).get().getFrequency();
    }

    /**
     * Sum of all costs normalized to a monthly cost
     */
    private Double getCycleCost(List<MonthlyBillCost> costs) {
        return costs.stream().mapToDouble(this::getCycleBillingCost).sum();
    }

    /**
     * Converts a monthly spendable amount into a per-paycheck value based on all incomes.
     *
     * @param spendableMonthly spendable monthly amount
     * @param incomes income records used to determine total pay periods
     * @return spendable amount per paycheck
     */
    private Double getSpendablePerPaycheckFromMonthly(Double spendableMonthly, List<IncomesModel> incomes) {
        if (incomes.isEmpty()) return (double) 0;
        var totalPeriodsPerYear = getTotalPayPeriodsPerYear(incomes);
        return spendableMonthly * (MONTHS_PER_YEAR / totalPeriodsPerYear);
    }

    /**
     * Sums the annual pay-period counts across all income sources.
     *
     * @param incomes income records to inspect
     * @return total pay periods per year across all incomes
     */
    private Double getTotalPayPeriodsPerYear(List<IncomesModel> incomes) {
        return incomes.stream()
                .map(e -> getFrequency(e.getFrequency()))
                .reduce(0.0, Double::sum);
    }

    /**
     * Converts a bill-like cost into a monthly equivalent based on its billing cycle.
     *
     * @param cost amount and billing cycle pair
     * @return monthly-normalized cost
     */
    private Double getCycleBillingCost(MonthlyBillCost cost) {
        // A credit card's minimum payment is optional; without one it adds nothing.
        if (cost.amount() == null) return 0.0;
        var cycle = getMonthlyBillingCycle(cost.billingCycle());
        return cost.amount() / cycle;
    }

    /**
     * Totals the outstanding balance across all debts.
     *
     * @param debts debt records to sum
     * @return total amount owed
     */
    private Double getTotalDebt(List<DebtsModel> debts) {
        return debts.stream().map(DebtsModel::getAmountOwed).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
    }

}
