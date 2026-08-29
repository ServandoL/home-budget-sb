package com.servando.homebudget.utils;

import com.servando.homebudget.models.database.BillCategory;
import com.servando.homebudget.models.database.BillingCycle;
import com.servando.homebudget.models.database.HouseRepairsStatus;
import com.servando.homebudget.models.database.PayFrequency;

import java.time.Instant;

public class ResolveValueFactory<TValue> {
    private final TValue request;
    private final TValue other;

    private ResolveValueFactory(TValue request, TValue other) {
        this.request = request;
        this.other = other;
    }

    private TValue resolveValue() {
        return this.request == null ? other : request;
    }

    public static HouseRepairsStatus of(HouseRepairsStatus request, HouseRepairsStatus other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }
    public static PayFrequency of(PayFrequency request, PayFrequency other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static BillCategory of(BillCategory request, BillCategory other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static String of(String request, String other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static Integer of(Integer request, Integer other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static BillingCycle of(BillingCycle request, BillingCycle other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static Instant of(Instant request, Instant other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }

    public static Double of(Double request, Double other) {
        return new ResolveValueFactory<>(request, other).resolveValue();
    }
}
