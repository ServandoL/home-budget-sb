package com.servando.homebudget.services;

import com.servando.homebudget.models.dto.GenericResponseDto;
import org.springframework.data.domain.Sort;

/** Every operation works in one household; {@code householdId} comes from the checked CurrentMember. */
public interface BaseCrudService<TModel, TResults, TCreateRequest, TUpdateRequest> {

    GenericResponseDto<TResults> getAll(String householdId, Sort sort);

    GenericResponseDto<TModel> create(String householdId, TCreateRequest request, TModel toCreate);

    GenericResponseDto<TModel> update(String householdId, String id, TUpdateRequest request, TModel toUpdate);

    GenericResponseDto<String> delete(String householdId, String id);
}
