package com.servando.homebudget.repository;

import com.servando.homebudget.models.dto.BillDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BillsRepository extends MongoRepository<BillDto, String> {
    Optional<BillDto> findByName(String name);
}
