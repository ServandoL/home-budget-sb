package com.servando.homebudget.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BaseCrudRepository<TModel> extends MongoRepository<TModel, String> {
    Optional<TModel> findByName(String name);
}
