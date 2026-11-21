package com.servando.homebudget.repository;


import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Every method takes the household, and there is no unscoped findAll or findById (this extends
 * Repository, not MongoRepository), so a query that forgets the household doesn't compile.
 */
@NoRepositoryBean
public interface BaseCrudRepository<TModel> extends Repository<TModel, String> {
    List<TModel> findAllByHouseholdId(String householdId, Sort sort);

    Optional<TModel> findByIdAndHouseholdId(String id, String householdId);

    /** Names are stored encrypted; find one by FieldCipher.nameIndex(householdId, name). */
    Optional<TModel> findByHouseholdIdAndNameIndex(String householdId, byte[] nameIndex);

    <S extends TModel> S save(S model);

    long deleteByIdAndHouseholdId(String id, String householdId);

    /** For the purge once a deleted household's restore window has passed. */
    long deleteByHouseholdId(String householdId);
}
