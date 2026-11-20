package com.servando.homebudget.services;

import com.home.household.HouseholdDataPurger;
import com.servando.homebudget.configuration.MongoConfiguration;
import com.servando.homebudget.models.database.SharedProperties;
import org.bson.types.ObjectId;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Deletes every budget record of a household, and its cached lists, once household-api's restore
 * window for a deleted household has passed. home-household-spring calls it daily for every such
 * household, so it does nothing once the data is gone.
 */
@Component
public class BudgetPurger implements HouseholdDataPurger {

    private final MongoTemplate mongo;
    private final CacheManager caches;

    public BudgetPurger(MongoTemplate mongo, CacheManager caches) {
        this.mongo = mongo;
        this.caches = caches;
    }

    @Override
    public long purge(String householdId) {
        long deleted = 0;
        for (Class<? extends SharedProperties> model : MongoConfiguration.MODELS) {
            deleted += mongo.remove(query(where("householdId").is(new ObjectId(householdId))), model)
                    .getDeletedCount();
        }
        for (String name : caches.getCacheNames()) {
            Cache cache = caches.getCache(name);
            if (cache != null) {
                cache.evict(householdId);
            }
        }
        return deleted;
    }
}
