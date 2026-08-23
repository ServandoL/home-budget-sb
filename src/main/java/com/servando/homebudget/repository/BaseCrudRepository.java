package com.servando.homebudget.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface BaseCrudRepository<TModel> extends MongoRepository<TModel, String> {
    Optional<TModel> findByName(String name);
}
