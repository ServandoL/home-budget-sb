package com.servando.homebudget.services;

import com.servando.homebudget.models.database.*;
import com.servando.homebudget.models.dto.DashboardDto;
import com.servando.homebudget.models.dto.MonthlyBillCost;
import com.servando.homebudget.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
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


    /**
     * Builds a dashboard snapshot by loading all financial records in parallel and deriving
     * monthly, weekly, and per-paycheck budget values from them.
     *
     * @param targetMonthlyTransfer the amount the user wants to transfer into savings or another goal each month
     * @param payPeriodBuffer additional buffer amount to reserve from each pay period
     * @return a populated dashboard summary
     */
    public DashboardDto getDashboard(Double targetMonthlyTransfer, Double payPeriodBuffer) {
        var dashboard = new DashboardDto();
        CompletableFuture<List<IncomesModel>> incomesFuture =
                CompletableFuture.supplyAsync(() -> incomesRepository.findAll(), executor);
        CompletableFuture<List<BillsModel>> billsFuture =
                CompletableFuture.supplyAsync(() -> billsRepository.findAll(), executor);
        CompletableFuture<List<SubscriptionsModel>> subsFuture =
                CompletableFuture.supplyAsync(() -> subscriptionsRepository.findAll(), executor);
        CompletableFuture<List<CreditCardsModel>> cardsFuture =
                CompletableFuture.supplyAsync(() -> creditCardsRepository.findAll(), executor);
        CompletableFuture<List<DebtsModel>> debtsFuture =
                CompletableFuture.supplyAsync(() -> debtsRepository.findAll(), executor);
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
        var recommendedTransferPerPayPeriod = getRecommendedTransferPerPayPeriod(incomes, targetMonthlyTransfer, payPeriodBuffer);
        var spendableMonthly = getSpendableMonthly(totalIncome, totalObligations, payPeriodBuffer, incomeFrequency);
        var spendablePerPaycheck = getSpendablePerPaycheckFromMonthly(spendableMonthly, incomes);
        var spendableWeekly = getSpendableWeeklyFromMonthly(spendableMonthly);
        dashboard.setMonthlyIncome(totalIncome);
        dashboard.setTargetMonthlyTransfer(targetMonthlyTransfer);
        dashboard.setBillsMonthly(billsMonthly);
        dashboard.setSubscriptionsMonthly(subscriptionsMonthly);
        dashboard.setCreditCardsMonthly(ccMonthly);
        dashboard.setTotalObligations(totalObligations);
        dashboard.setRecommendedTransfer(recommendedTransferPerPayPeriod);
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
     * Converts a monthly transfer target into a per-pay-period amount using the highest income frequency
     * available in the dashboard.
     *
     * @param incomes income records used to determine pay frequency
     * @param targetMonthlyTransfer desired monthly transfer target
     * @param buffer extra amount to add per pay period
     * @return recommended transfer amount per pay period
     */
    private Double getRecommendedTransferPerPayPeriod(List<IncomesModel> incomes, Double targetMonthlyTransfer, Double buffer) {
        if (incomes.isEmpty()) return (double) 0;
        var frequency = getIncomesFrequency(incomes);
        return (targetMonthlyTransfer / (getFrequency(frequency) / MONTHS_PER_YEAR)) + buffer;
    }

    /**
     * Calculates how much money remains available each month after obligations and any configured buffer.
     *
     * @param monthlyIncome total monthly income
     * @param totalObligations monthly obligations
     * @param buffer optional pay-period buffer to subtract from the monthly spendable amount
     * @param frequency pay frequency used to normalize the buffer
     * @return spendable monthly amount
     */
    private Double getSpendableMonthly(Double monthlyIncome, Double totalObligations, Double buffer, PayFrequency frequency) {
        if (buffer != null && buffer > 0) {
            var freq = getFrequency(frequency) / MONTHS_PER_YEAR;
            return (monthlyIncome - totalObligations) - (buffer / freq);
        }
        return monthlyIncome - totalObligations;
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
        return switch (freq) {
            case WEEKLY -> WEEKS_PER_YEAR;
            case BI_WEEKLY -> biweeklyFrequency;
            default -> semimonthlyFrequency;
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
     * @param billingCycle billing cycle to convert
     * @return month multiplier for the cycle
     */
    private Double getMonthlyBillingCycle(BillingCycle billingCycle) {
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
        return debts.stream().mapToDouble(DebtsModel::getAmountOwed).sum();
    }

}
