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
import java.util.function.Function;

/**
 * The household's budget in one place: what it needs each month to cover its expenses, what each
 * payday moves to the bills account to meet that, and what's left to spend.
 *
 * <p>Every amount is turned into a monthly figure. Each payday moves the same share of its paycheck,
 * enough that the paydays every month brings (two biweekly, four weekly) pay the month's bills. An
 * extra payday (a third biweekly or fifth weekly one) moves only the buffer. Every payday moves the
 * buffer, the cushion that grows the bills account a little over time. When two paychecks can't cover
 * the bills, every payday moves the same amount and the short months draw on the bills account.
 */
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
    private static final double MONTHS_PER_YEAR = 12.0;
    private static final double WEEKS_PER_YEAR = 52.0;
    /** Sums of decimals drift (4522.55 + 178.56 + 229.00 is 4930.110000000001); less than this is nothing. */
    private static final double HALF_A_CENT = 0.005;

    /**
     * Builds a dashboard snapshot by loading all financial records in parallel and deriving
     * monthly, weekly, and per-paycheck budget values from them.
     *
     * @param householdId the household whose records to total
     * @param targetMonthlyTransfer the amount the household moves into its bills account each month;
     *                              when null, it's the monthly obligations
     * @param payPeriodBuffer extra amount every paycheck moves to the bills account
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

        // An income has to pay something; a $0 or negative one from before that was checked is left out.
        List<IncomesModel> incomes = incomesFuture.join().stream()
                .filter(e -> e.getNetAmount() != null && e.getNetAmount() > 0 && e.getFrequency() != null)
                .toList();
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
        // A minimum payment is due every month, whatever billing cycle the card was saved with.
        var ccMonthly = sum(creditCards, CreditCardsModel::getMinimumPayment);
        var totalObligations = billsMonthly + subscriptionsMonthly + ccMonthly;
        var totalIncome = calculateMonthly(incomes);
        var buffer = payPeriodBuffer == null ? 0.0 : payPeriodBuffer;
        var effectiveTargetMonthlyTransfer = targetMonthlyTransfer != null ? targetMonthlyTransfer : totalObligations;
        var transferShortfall = getTransferShortfall(totalObligations, effectiveTargetMonthlyTransfer);
        // The transfers move the target, or the bills when the target is less: they have to be paid.
        var toMoveMonthly = Math.max(effectiveTargetMonthlyTransfer, totalObligations);
        var shortMonth = incomes.stream()
                .mapToDouble(e -> e.getNetAmount() * getShortMonthPaychecks(e.getFrequency())).sum();
        // The paydays every month brings pay the bills, so an extra payday only moves the buffer.
        // Otherwise the bills are spread over every payday in the year, the extra ones too.
        var shortMonthsCover = shortMonth >= toMoveMonthly - HALF_A_CENT;
        var share = shortMonthsCover
                ? (shortMonth > 0 ? toMoveMonthly / shortMonth : 0.0)
                : (totalIncome > 0 ? toMoveMonthly / totalIncome : 0.0);
        var transfers = getTransfers(incomes, share, buffer, shortMonthsCover);
        // Without income nothing is transferred to fall short; over budget says the bills aren't covered.
        var shortMonthShortfall = shortMonthsCover || incomes.isEmpty() ? 0.0 : toMoveMonthly - shortMonth * share;

        // Without income there's no paycheck to transfer from, and the bills come out of what's there.
        var movedMonthly = incomes.isEmpty() ? totalObligations : transfers.stream()
                .mapToDouble(t -> t.perPaycheck() * regularPaydays(t) + t.extraPaydayTransfer() * t.extraPaydaysYearly())
                .sum() / MONTHS_PER_YEAR;
        var spendableMonthly = totalIncome - movedMonthly;

        dashboard.setMonthlyIncome(totalIncome);
        dashboard.setTargetMonthlyTransfer(effectiveTargetMonthlyTransfer);
        dashboard.setTransferShortfall(transferShortfall);
        dashboard.setTransferCoversObligations(transferShortfall == 0.0);
        dashboard.setBillsMonthly(billsMonthly);
        dashboard.setSubscriptionsMonthly(subscriptionsMonthly);
        dashboard.setCreditCardsMonthly(ccMonthly);
        dashboard.setTotalObligations(totalObligations);
        dashboard.setTransfers(transfers);
        dashboard.setRecommendedTransfer(theOneValue(transfers, IncomeTransfer::perPaycheck));
        dashboard.setSpendablePerPaycheck(theOneValue(transfers, IncomeTransfer::spendablePerPaycheck));
        dashboard.setShortMonthShortfall(shortMonthShortfall);
        dashboard.setShortMonthsCushion(shortMonthShortfall * incomes.stream()
                .mapToInt(e -> getLongestRunOfShortMonths(e.getFrequency())).max().orElse(0));
        dashboard.setSpendableMonthly(spendableMonthly);
        dashboard.setSpendableWeekly(spendableMonthly * MONTHS_PER_YEAR / WEEKS_PER_YEAR);
        dashboard.setIsOverBudget(spendableMonthly < -HALF_A_CENT);
        var totalDebts = sum(debts, DebtsModel::getAmountOwed);
        var cardBalances = sum(creditCards, CreditCardsModel::getCurrentBalance);
        dashboard.setTotalDebts(totalDebts);
        dashboard.setCreditCardBalances(cardBalances);
        dashboard.setTotalOwed(totalDebts + cardBalances);
        dashboard.setSubscriptionCounts(subscriptions.size());
        dashboard.setCreditCardCounts(creditCards.size());
        dashboard.setBillCounts(bills.size());
        dashboard.setFrequency(getIncomesFrequency(incomes));
        return dashboard;
    }

    /**
     * How much a month the target falls short of the obligations. A target typed to match them counts
     * as covering them: anything under half a cent is no shortfall.
     */
    private Double getTransferShortfall(Double totalObligations, Double targetMonthlyTransfer) {
        var shortfall = totalObligations - targetMonthlyTransfer;
        return shortfall < HALF_A_CENT ? 0.0 : shortfall;
    }

    /**
     * The transfer for each income: the same share of every paycheck, so incomes on different schedules
     * or of different sizes each pay their part, plus the buffer. And what that leaves of the paycheck.
     * On an extra payday it's just the buffer when the other paydays already pay the bills.
     */
    private List<IncomeTransfer> getTransfers(List<IncomesModel> incomes, double share, double buffer,
                                              boolean shortMonthsCover) {
        return incomes.stream()
                .map(income -> {
                    var transfer = income.getNetAmount() * share + buffer;
                    var extraPaydays = (int) Math.round(getPaychecksPerYear(income.getFrequency())
                            - getShortMonthPaychecks(income.getFrequency()) * MONTHS_PER_YEAR);
                    return new IncomeTransfer(income.getId(), income.getName(), income.getFrequency(),
                            transfer, income.getNetAmount() - transfer, extraPaydays,
                            shortMonthsCover ? buffer : transfer);
                })
                .toList();
    }

    /** The paydays a year that every month brings: 24 for biweekly, 48 for weekly. */
    private static double regularPaydays(IncomeTransfer t) {
        return getPaychecksPerYear(t.frequency()) - t.extraPaydaysYearly();
    }

    /** The value every paycheck has in common, or null when there's no paycheck or they differ. */
    private static Double theOneValue(List<IncomeTransfer> transfers, Function<IncomeTransfer, Double> value) {
        if (transfers.isEmpty()) return null;
        var first = value.apply(transfers.getFirst());
        return transfers.stream().allMatch(t -> Math.abs(value.apply(t) - first) < HALF_A_CENT) ? first : null;
    }

    /** Sums all incomes after converting each one to a monthly equivalent. */
    private double calculateMonthly(List<IncomesModel> incomes) {
        return incomes.stream().mapToDouble(e -> e.getNetAmount() * getPaychecksPerYear(e.getFrequency())).sum()
                / MONTHS_PER_YEAR;
    }

    /** Every value named, no default: a new frequency must be counted here. */
    private static double getPaychecksPerYear(PayFrequency freq) {
        return switch (freq) {
            case WEEKLY -> WEEKS_PER_YEAR;
            case BI_WEEKLY -> 26.0;
            case SEMI_MONTHLY -> 24.0;
            case MONTHLY -> MONTHS_PER_YEAR;
        };
    }

    /**
     * The most months in a row that bring only the fewest paychecks, worked out over every start date.
     * With several schedules the longest one counts, which overstates the cushion rather than under.
     */
    private static int getLongestRunOfShortMonths(PayFrequency freq) {
        return switch (freq) {
            case WEEKLY -> 3;
            case BI_WEEKLY -> 6;
            case SEMI_MONTHLY, MONTHLY -> 0;
        };
    }

    /** The fewest paychecks a calendar month can bring. */
    private static double getShortMonthPaychecks(PayFrequency freq) {
        return switch (freq) {
            case WEEKLY -> 4.0;
            case BI_WEEKLY, SEMI_MONTHLY -> 2.0;
            case MONTHLY -> 1.0;
        };
    }

    /**
     * Normalizes a billing cycle to the number of months it spans. Bills and subscriptions can't be
     * saved as optional any more; one saved before that is spread over a year, as the app shows it.
     *
     * @param billingCycle billing cycle to convert; none counts as monthly, as the UI shows it
     */
    private static double getMonthlyBillingCycle(BillingCycle billingCycle) {
        if (billingCycle == null) return 1.0;
        return switch (billingCycle) {
            case MONTHLY -> 1.0;
            case SEMIANNUAL -> 6.0;
            case ANNUAL, OPTIONAL -> MONTHS_PER_YEAR;
        };
    }

    /**
     * The most frequent pay schedule among the incomes, for the app to name "payday" by. Bi-weekly
     * when there's no income. Only shown; nothing is worked out from it.
     */
    private PayFrequency getIncomesFrequency(List<IncomesModel> incomes) {
        return incomes.stream()
                .max(Comparator.comparingDouble(e -> getPaychecksPerYear(e.getFrequency())))
                .map(IncomesModel::getFrequency)
                .orElse(PayFrequency.BI_WEEKLY);
    }

    /** Sum of all costs normalized to a monthly cost; a missing amount adds nothing. */
    private double getCycleCost(List<MonthlyBillCost> costs) {
        return costs.stream()
                .filter(c -> c.amount() != null)
                .mapToDouble(c -> c.amount() / getMonthlyBillingCycle(c.billingCycle()))
                .sum();
    }

    private static <T> double sum(List<T> records, Function<T, Double> amount) {
        return records.stream().map(amount).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
    }
}
