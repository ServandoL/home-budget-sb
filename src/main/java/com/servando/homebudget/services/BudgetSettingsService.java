package com.servando.homebudget.services;

import com.home.household.CurrentMember;
import com.servando.homebudget.models.database.BudgetSettingsModel;
import com.servando.homebudget.models.dto.BudgetSettingsDto;
import com.servando.homebudget.models.dto.UpdateBudgetSettingsDto;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/** One set of planning settings per household, which the dashboard uses on every device. */
@Service
public class BudgetSettingsService {
    /** What a household gets until it saves its own: a calculated target and a $100 buffer. */
    public static final double DEFAULT_PAY_PERIOD_BUFFER = 100.0;

    private final MongoTemplate mongo;

    public BudgetSettingsService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public BudgetSettingsDto get(String householdId) {
        var saved = find(householdId);
        if (saved == null) {
            return new BudgetSettingsDto(null, DEFAULT_PAY_PERIOD_BUFFER, true, null, null);
        }
        return new BudgetSettingsDto(saved.getTargetMonthlyTransfer(), saved.getPayPeriodBuffer(), false,
                saved.getUpdatedAt(), saved.getUpdatedBy());
    }

    /**
     * Saves through the entity, never an update query, so the listener encrypts the amounts. Two
     * first saves at once: the unique index stops the second, which then updates the first's record.
     */
    public BudgetSettingsDto save(CurrentMember member, UpdateBudgetSettingsDto settings) {
        try {
            return write(member, settings);
        } catch (DuplicateKeyException raced) {
            return write(member, settings);
        }
    }

    private BudgetSettingsDto write(CurrentMember member, UpdateBudgetSettingsDto settings) {
        var model = find(member.householdId());
        if (model == null) {
            model = new BudgetSettingsModel();
            model.setHouseholdId(member.householdId());
        }
        model.setTargetMonthlyTransfer(settings.targetMonthlyTransfer());
        model.setPayPeriodBuffer(settings.payPeriodBuffer());
        model.setUpdatedAt(Instant.now());
        model.setUpdatedBy(member.email());
        mongo.save(model);
        return get(member.householdId());
    }

    /** Back to the defaults. Also what the purge uses. */
    public long reset(String householdId) {
        return mongo.remove(query(where("householdId").is(new ObjectId(householdId))), BudgetSettingsModel.class)
                .getDeletedCount();
    }

    private BudgetSettingsModel find(String householdId) {
        return mongo.findOne(query(where("householdId").is(new ObjectId(householdId))), BudgetSettingsModel.class);
    }
}
