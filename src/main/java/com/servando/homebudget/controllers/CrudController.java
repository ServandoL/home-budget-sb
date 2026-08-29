package com.servando.homebudget.controllers;

import com.servando.homebudget.models.dto.GenericResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface CrudController<TModel, TCreateRequest, TUpdateRequest> {
    ResponseEntity<GenericResponseDto<List<TModel>>> getAll();

    ResponseEntity<GenericResponseDto<TModel>> create(@RequestBody @Valid TCreateRequest toCreate);

    ResponseEntity<GenericResponseDto<TModel>> update(@PathVariable @Valid String id, @RequestBody @Valid TUpdateRequest toUpdate);

    ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id);

}
