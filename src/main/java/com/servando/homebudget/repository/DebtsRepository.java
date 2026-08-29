package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.DebtsModel;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtsRepository extends BaseCrudRepository<DebtsModel> {
}
