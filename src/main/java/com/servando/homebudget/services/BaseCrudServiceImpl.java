package com.servando.homebudget.services;

import com.servando.homebudget.encryption.FieldCipher;
import com.servando.homebudget.exceptions.RecordAlreadyExistsException;
import com.servando.homebudget.exceptions.RecordNotFoundException;
import com.servando.homebudget.models.database.SharedProperties;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.SharedRequestProperties;
import com.servando.homebudget.repository.BaseCrudRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * CRUD within one household. A record in another household is "not found", whether it exists
 * or not, and names only have to be unique within a household.
 */
public abstract class BaseCrudServiceImpl<TModel extends SharedProperties, TRepository extends BaseCrudRepository<TModel>, TCreateRequest extends SharedRequestProperties, TUpdateRequest extends SharedRequestProperties> implements BaseCrudService<TModel, List<TModel>, TCreateRequest, TUpdateRequest> {
    protected TRepository repository;
    private final FieldCipher cipher;

    protected BaseCrudServiceImpl(TRepository repository, FieldCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    @Override
    public GenericResponseDto<List<TModel>> getAll(String householdId, Sort sort) {
        var results = repository.findAllByHouseholdId(householdId, sort);
        return new GenericResponseDto<>(
                true,
                "Successfully retrieved records",
                results
        );
    }

    /** For an order on an encrypted field, which MongoDB can't sort by. */
    public GenericResponseDto<List<TModel>> getAll(String householdId, Comparator<TModel> order) {
        var results = new ArrayList<>(repository.findAllByHouseholdId(householdId, Sort.unsorted()));
        results.sort(order);
        return new GenericResponseDto<>(true, "Successfully retrieved records", results);
    }

    /** Largest first, records without the value last, as MongoDB's descending sort did. */
    protected static <TModel> Comparator<TModel> largestFirst(Function<TModel, ? extends Number> value) {
        return Comparator.comparing((TModel m) -> value.apply(m) == null ? null : value.apply(m).doubleValue(),
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Optional<TModel> findByName(String householdId, String name) {
        return repository.findByHouseholdIdAndNameIndex(householdId, cipher.nameIndex(householdId, name));
    }

    @Override
    public GenericResponseDto<TModel> create(String householdId, TCreateRequest tCreateRequest, TModel toCreate) {
        var result = findByName(householdId, tCreateRequest.getName());
        if (result.isPresent()) {
            throw new RecordAlreadyExistsException(result.get().getName());
        }
        toCreate.setHouseholdId(householdId);
        var created = save(toCreate);
        return new GenericResponseDto<>(true, "Record created", created);
    }

    @Override
    public GenericResponseDto<TModel> update(String householdId, String id, TUpdateRequest tUpdateRequest, TModel toUpdate) {
        var existing = findOtherById(householdId, id);
        // prevent duplicate names if name can change
        if (tUpdateRequest.getName() != null && !existing.getName().equals(tUpdateRequest.getName())) {
            var found = findByName(householdId, tUpdateRequest.getName());
            if (found.isPresent() && !found.get().getId().equals(id)) {
                throw new RecordAlreadyExistsException(tUpdateRequest.getName());
            }
        }
        toUpdate.setId(id);
        toUpdate.setHouseholdId(householdId);
        save(toUpdate);
        return new GenericResponseDto<>(true, "Record updated", toUpdate);
    }

    @Override
    public GenericResponseDto<String> delete(String householdId, String id) {
        repository.deleteByIdAndHouseholdId(id, householdId);
        return new GenericResponseDto<>(true, "Record deleted", id);
    }

    /** The record if it's in the household; otherwise not found. */
    public TModel findOtherById(String householdId, String id) {
        return repository.findByIdAndHouseholdId(id, householdId)
                .orElseThrow(() -> new RecordNotFoundException(id));
    }

    /** The unique (householdId, name) index catches two saves of one name at once. */
    private TModel save(TModel model) {
        try {
            return repository.save(model);
        } catch (DuplicateKeyException e) {
            throw new RecordAlreadyExistsException(model.getName());
        }
    }
}
