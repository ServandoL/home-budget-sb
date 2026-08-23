package com.servando.homebudget.repository;

import com.servando.homebudget.models.dto.CreditCardDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditCardsRepository extends MongoRepository<CreditCardDto, String> {
    Optional<CreditCardDto> findByName(String name);
}
