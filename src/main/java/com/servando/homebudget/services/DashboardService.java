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


    public DashboardDto getDashboard(Double targetMonthlyTransfer, Double buffer) {
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
        var totalObligations = billsMonthly + subscriptionsMonthly + ccMonthly;
        var recommendedTransfer = getRecommendedTransfer(incomes, targetMonthlyTransfer, buffer);
        var spendableMonthly = getSpendableMonthly(calculateMonthly(incomes), totalObligations);
        var spendablePerPaycheck = getSpendablePerPaycheckFromMonthly(spendableMonthly, incomes);
        dashboard.setMonthlyIncome(calculateMonthly(incomes));
        dashboard.setTargetMonthlyTransfer(targetMonthlyTransfer);
        dashboard.setBillsMonthly(billsMonthly);
        dashboard.setSubscriptionsMonthly(subscriptionsMonthly);
        dashboard.setCreditCardsMonthly(ccMonthly);
        dashboard.setTotalObligations(totalObligations);
        dashboard.setRecommendedTransfer(recommendedTransfer);
        dashboard.setSpendablePerPaycheck(spendablePerPaycheck);
        dashboard.setSpendableMonthly(spendableMonthly);
        dashboard.setSpendableWeekly(getSpendableWeeklyFromMonthly(spendableMonthly));
        dashboard.setIsOverBudget(dashboard.getSpendableMonthly() < (double) 0);
        dashboard.setTotalDebts(getTotalDebt(debts));
        dashboard.setSubscriptionCounts(subscriptions.size());
        dashboard.setCreditCardCounts(creditCards.size());
        dashboard.setBillCounts(bills.size());
        dashboard.setFrequency(getIncomesFrequency(incomes));
        return dashboard;
    }

    private Double getSpendableMonthly(Double monthlyIncome, Double totalObligations) {
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
        return getAnnualIncome(income) / 12;
    }

    private Integer getFrequency(PayFrequency freq) {
        int weeklyFrequency = 52;
        int biweeklyFrequency = 26;
        int semimonthlyFrequency = 24;
        return switch (freq) {
            case WEEKLY -> weeklyFrequency;
            case BI_WEEKLY -> biweeklyFrequency;
            default -> semimonthlyFrequency;
        };
    }

    /**
     * (monthlyIncome − obligations) × 12 ÷ 52
     */
    private Double getSpendableWeeklyFromMonthly(Double spendableMonthly) {
        return (spendableMonthly * 12) / 52;
    }

    private Integer getMonthlyBillingCycle(BillingCycle billingCycle) {
        return switch (billingCycle) {
            case ANNUAL, OPTIONAL -> 12;
            case SEMIANNUAL -> 6;
            default -> 1;
        };
    }

    /**
     * Recommended transfer per pay period = monthly target ÷ pay periods per month
     */
    private Double getRecommendedTransfer(List<IncomesModel> incomes, Double targetMonthlyTransfer, Double buffer) {
        if (incomes.isEmpty()) return (double) 0;
        var frequency = getIncomesFrequency(incomes);
        return (targetMonthlyTransfer / (getFrequency(frequency) / 12.0)) + buffer;
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
        return spendableMonthly * (12.0 / totalPeriodsPerYear);
    }
//    Monthly Income=B3=$7,949.96, Monthly Expenses=E2=$4,930.11
//    =(((B3-E2)*12)/52)*2
    private Double getSpendablePerPaycheck(Double monthlyIncome, Double monthlyExpense) {
        return (((monthlyIncome - monthlyExpense)*12)/52) * 2;
    }

    private Integer getTotalPayPeriodsPerYear(List<IncomesModel> incomes) {
        return incomes.stream()
                .map(e -> getFrequency(e.getFrequency()))
                .reduce(0, Integer::sum);
    }

    private Double getCycleBillingCost(MonthlyBillCost cost) {
        var cycle = getMonthlyBillingCycle(cost.billingCycle());
        return cost.amount() / cycle;
    }

    private Double getTotalDebt(List<DebtsModel> debts) {
        return debts.stream().mapToDouble(DebtsModel::getAmountOwed).sum();
    }

}
