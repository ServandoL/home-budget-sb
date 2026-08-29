package com.servando.homebudget.services;

import com.servando.homebudget.exceptions.RecordAlreadyExistsException;
import com.servando.homebudget.exceptions.RecordNotFoundException;
import com.servando.homebudget.models.database.SharedProperties;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.SharedRequestProperties;
import com.servando.homebudget.repository.BaseCrudRepository;

import java.util.List;

public abstract class BaseCrudServiceImpl<TModel extends SharedProperties, TRepository extends BaseCrudRepository<TModel>, TCreateRequest extends SharedRequestProperties, TUpdateRequest extends SharedRequestProperties> implements BaseCrudService<TModel, List<TModel>, TCreateRequest, TUpdateRequest> {
    protected TRepository repository;

    protected BaseCrudServiceImpl(TRepository repository) {
        this.repository = repository;
    }

    @Override
    public GenericResponseDto<List<TModel>> getAll() {
        var results = repository.findAll();
        return new GenericResponseDto<>(
                true,
                "Successfully retrieved records",
                results
        );
    }

    @Override
    public GenericResponseDto<TModel> create(TCreateRequest tCreateRequest, TModel toCreate) {
        var result = repository.findByName(tCreateRequest.getName());
        if (result.isPresent()) {
            throw new RecordAlreadyExistsException(result.get().getName());
        }
        var created = repository.save(toCreate);
        return new GenericResponseDto<>(true, "Record created", created);
    }

    @Override
    public GenericResponseDto<String> update(String id, TUpdateRequest tUpdateRequest, TModel toUpdate) {
        var existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new RecordNotFoundException(id);
        }
        // prevent duplicate names if name can change
        if (!existing.get().getName().equals(tUpdateRequest.getName())) {
            var found = repository.findByName(tUpdateRequest.getName());
            if (found.isPresent()) {
                var other = found.get();
                if (!other.getId().equals(id)) {
                    throw new RecordAlreadyExistsException(tUpdateRequest.getName());
                }
            }
        }
        toUpdate.setId(id);
        repository.save(toUpdate);
        return new GenericResponseDto<>(true, "Record updated", toUpdate.getId());
    }

    @Override
    public GenericResponseDto<String> delete(String id) {
        repository.deleteById(id);
        return new GenericResponseDto<>(true, "Record deleted", id);
    }

    public TModel findOtherById(String id) {
        var existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new RecordNotFoundException(id);
        }
        return existing.get();
    }
}
