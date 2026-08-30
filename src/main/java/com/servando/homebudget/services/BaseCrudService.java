package com.servando.homebudget.services;

import com.servando.homebudget.models.dto.GenericResponseDto;
import org.springframework.data.domain.Sort;

public interface BaseCrudService<TModel, TResults, TCreateRequest, TUpdateRequest> {

    GenericResponseDto<TResults> getAll();
    GenericResponseDto<TResults> getAll(Sort sort);

    GenericResponseDto<TModel> create(TCreateRequest request, TModel toCreate);

    GenericResponseDto<TModel> update(String id, TUpdateRequest request, TModel toUpdate);

    GenericResponseDto<String> delete(String id);
}
