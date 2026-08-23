package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.IncomesModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomesRepository extends MongoRepository<IncomesModel, String> {
    Optional<IncomesModel> findByName(String name);
}
