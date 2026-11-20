package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.services.IncomeServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incomes")
public class IncomesControllerImpl implements CrudController<IncomesModel, CreateIncomeRequestDto, UpdateIncomeRequestDto> {

    @Autowired
    private IncomeServiceImpl incomeService;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<IncomesModel>>> getAll(CurrentMember member) {
        var results = incomeService.findAll(member.householdId());
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<IncomesModel>> create(CurrentMember member, @RequestBody @Valid CreateIncomeRequestDto toCreate) {
        var response = incomeService.createIncome(member.householdId(), toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<IncomesModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid UpdateIncomeRequestDto toUpdate) {
        var response = incomeService.updateIncome(member.householdId(), id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id) {
        var result = incomeService.delete(member.householdId(), id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
