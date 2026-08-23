package com.servando.homebudget.repository;

import com.servando.homebudget.models.dto.IncomeDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomesRepository extends MongoRepository<IncomeDto, String> {
    Optional<IncomeDto> findByName(String name);
}
