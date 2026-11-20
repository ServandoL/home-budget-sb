package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
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
    public ResponseEntity<GenericResponseDto<List<SubscriptionsModel>>> getAll(CurrentMember member) {
        var results = subscriptionsService.findAll(member.householdId());
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<SubscriptionsModel>> create(CurrentMember member, @RequestBody @Valid CreateSubscriptionsRequestDto toCreate) {
        var response = subscriptionsService.createIncome(member.householdId(), toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<SubscriptionsModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid UpdateSubscriptionsRequestDto toUpdate) {
        var response = subscriptionsService.updateIncome(member.householdId(), id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id) {
        var result = subscriptionsService.delete(member.householdId(), id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
