package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.BillsModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BillsRepository extends MongoRepository<BillsModel, String> {
    Optional<BillsModel> findByName(String name);
}
