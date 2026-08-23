package com.servando.homebudget.repository;

import com.servando.homebudget.models.database.CreditCardsModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditCardsRepository extends MongoRepository<CreditCardsModel, String> {
    Optional<CreditCardsModel> findByName(String name);
}
