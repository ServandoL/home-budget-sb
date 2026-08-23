package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.SubscriptionsModel;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionsRepository extends BaseCrudRepository<SubscriptionsModel> {
}
