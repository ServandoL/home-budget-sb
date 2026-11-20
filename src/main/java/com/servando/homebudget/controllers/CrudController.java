package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.servando.homebudget.models.dto.GenericResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface CrudController<TModel, TCreateRequest, TUpdateRequest> {
    ResponseEntity<GenericResponseDto<List<TModel>>> getAll(CurrentMember member);

    ResponseEntity<GenericResponseDto<TModel>> create(CurrentMember member, @RequestBody @Valid TCreateRequest toCreate);

    ResponseEntity<GenericResponseDto<TModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid TUpdateRequest toUpdate);

    ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id);

}
