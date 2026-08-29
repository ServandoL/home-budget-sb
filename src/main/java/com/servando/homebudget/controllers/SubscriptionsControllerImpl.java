package com.servando.homebudget.controllers;

import com.servando.homebudget.models.database.SubscriptionsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.services.SubscriptionsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionsControllerImpl implements CrudController<SubscriptionsModel, CreateSubscriptionsRequestDto, UpdateSubscriptionsRequestDto> {

    @Autowired
    private SubscriptionsServiceImpl subscriptionsService;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<SubscriptionsModel>>> getAll() {
        var results = subscriptionsService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<SubscriptionsModel>> create(@RequestBody @Valid CreateSubscriptionsRequestDto toCreate) {
        var response = subscriptionsService.createIncome(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<SubscriptionsModel>> update(@PathVariable @Valid String id, @RequestBody @Valid UpdateSubscriptionsRequestDto toUpdate) {
        var response = subscriptionsService.updateIncome(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id) {
        var result = subscriptionsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
