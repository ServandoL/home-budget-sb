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


    public DashboardDto getDashboard() {
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
        dashboard.setMonthlyIncome(calculateMonthly(incomes));
        dashboard.setBillsMonthly(billsMonthly);
        dashboard.setSubscriptionsMonthly(subscriptionsMonthly);
        dashboard.setCreditCardsMonthly(ccMonthly);
        dashboard.setTotalObligations(totalObligations);
        dashboard.setRecommendedTransfer(getRecommendedTransfer(incomes, totalObligations));
        dashboard.setSpendablePerPaycheck(getSpendablePerPaycheck(incomes, totalObligations));
        dashboard.setSpendableWeekly(getSpendableWeekly(dashboard.getMonthlyIncome(), totalObligations));
        dashboard.setSpendableMonthly(getSpendableMonthly(dashboard.getMonthlyIncome(), totalObligations));
        dashboard.setIsOverBudget(dashboard.getSpendableMonthly() < (double) 0);
        dashboard.setTotalDebts(getTotalDebt(debts));
        dashboard.setSubscriptionCounts(subscriptions.size());
        dashboard.setCreditCardCounts(creditCards.size());
        dashboard.setBillCounts(bills.size());
        dashboard.setFrequency(getIncomesFrequency(incomes));
        return dashboard;
    }

    /**
     * Monthly income minus total monthly obligations
     */
    private Double getSpendableMonthly(Double monthlyIncome, Double obligations) {
        return monthlyIncome - obligations;
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
    private Double getSpendableWeekly(Double monthlyIncome, Double obligations) {
        return ((monthlyIncome - obligations) * 12) / 52;
    }

    private Integer getMonthlyBillingCycle(BillingCycle billingCycle) {
        return switch (billingCycle) {
            case ANNUAL, OPTIONAL -> 12;
            case SEMIANNUAL -> 6;
            default -> 1;
        };
    }

    /**
     * Recommended Fidelity → bills transfer per pay period = obligations × 12 ÷ periodsPerYear
     */
    private Double getRecommendedTransfer(List<IncomesModel> incomes, Double obligations) {
        if (incomes.isEmpty()) return (double) 0;
        var frequency = getIncomesFrequency(incomes);
        return (obligations * 12) / getFrequency(frequency);
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

    /**
     * Net paycheck minus the recommended transfer
     */
    private Double getSpendablePerPaycheck(List<IncomesModel> incomes, Double obligations) {
        if (incomes.isEmpty()) return (double) 0;
        var totalAnnualIncome = incomes.stream().mapToDouble(this::getAnnualIncome).sum();
        var totalPeriodsPerYear = incomes.
                stream()
                .map(e -> getFrequency(e.getFrequency()))
                .reduce(0, Integer::sum);
        var averageNetPerPaycheck = totalAnnualIncome / totalPeriodsPerYear;
        return averageNetPerPaycheck - getRecommendedTransfer(incomes, obligations);
    }

    private Double getCycleBillingCost(MonthlyBillCost cost) {
        var cycle = getMonthlyBillingCycle(cost.billingCycle());
        return cost.amount() / cycle;
    }

    private Double getTotalDebt(List<DebtsModel> debts) {
        return debts.stream().mapToDouble(DebtsModel::getAmountOwed).sum();
    }

}
