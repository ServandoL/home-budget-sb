package com.servando.homebudget.repository;

import com.servando.homebudget.models.dto.SubscriptionDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionsRepository extends MongoRepository<SubscriptionDto, String> {
    Optional<SubscriptionDto> findByName(String name);
}
