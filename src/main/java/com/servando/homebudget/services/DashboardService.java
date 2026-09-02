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
     * Recommended transfer per pay period = monthly target ÷ pay periods per month
     */
    private Double getRecommendedTransferPerPayPeriod(List<IncomesModel> incomes, Double targetMonthlyTransfer, Double buffer) {
        if (incomes.isEmpty()) return (double) 0;
        var frequency = getIncomesFrequency(incomes);
        return (targetMonthlyTransfer / (getFrequency(frequency) / MONTHS_PER_YEAR)) + buffer;
    }

    private Double getSpendableMonthly(Double monthlyIncome, Double totalObligations, Double buffer, PayFrequency frequency) {
        if (buffer != null && buffer > 0) {
            var freq = getFrequency(frequency) / MONTHS_PER_YEAR;
            return (monthlyIncome - totalObligations) - (buffer / freq);
        }
        return monthlyIncome - totalObligations;
    }

    private Double calculateMonthly(List<IncomesModel> incomes) {
        if (incomes.isEmpty()) return (double) 0;
        return incomes.stream().mapToDouble(this::getMonthlyIncome).sum();
    }

    private Double getAnnualIncome(IncomesModel income) {
        var frequency = getFrequency(income.getFrequency());
        return income.getNetAmount() * frequency;
    }

    private Double getMonthlyIncome(IncomesModel income) {
        return getAnnualIncome(income) / MONTHS_PER_YEAR;
    }

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

    private Double getMonthlyBillingCycle(BillingCycle billingCycle) {
        return switch (billingCycle) {
            case ANNUAL, OPTIONAL -> MONTHS_PER_YEAR;
            case SEMIANNUAL -> 6.0;
            default -> 1.0;
        };
    }

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

    private Double getSpendablePerPaycheckFromMonthly(Double spendableMonthly, List<IncomesModel> incomes) {
        if (incomes.isEmpty()) return (double) 0;
        var totalPeriodsPerYear = getTotalPayPeriodsPerYear(incomes);
        return spendableMonthly * (MONTHS_PER_YEAR / totalPeriodsPerYear);
    }

    private Double getTotalPayPeriodsPerYear(List<IncomesModel> incomes) {
        return incomes.stream()
                .map(e -> getFrequency(e.getFrequency()))
                .reduce(0.0, Double::sum);
    }

    private Double getCycleBillingCost(MonthlyBillCost cost) {
        var cycle = getMonthlyBillingCycle(cost.billingCycle());
        return cost.amount() / cycle;
    }

    private Double getTotalDebt(List<DebtsModel> debts) {
        return debts.stream().mapToDouble(DebtsModel::getAmountOwed).sum();
    }

}
