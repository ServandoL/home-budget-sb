package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.EventBudgetModel;
import org.springframework.stereotype.Repository;

@Repository
public interface EventBudgetsRepository extends BaseCrudRepository<EventBudgetModel> {
}
