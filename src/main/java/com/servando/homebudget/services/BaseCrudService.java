package com.servando.homebudget.services;

import com.servando.homebudget.models.dto.GenericResponseDto;

public interface BaseCrudService<TModel, TResults, TCreateRequest, TUpdateRequest> {

    GenericResponseDto<TResults> getAll();

    GenericResponseDto<TModel> create(TCreateRequest request, TModel toCreate);

    GenericResponseDto<String> update(String id, TUpdateRequest request, TModel toUpdate);

    GenericResponseDto<String> delete(String id);
}
