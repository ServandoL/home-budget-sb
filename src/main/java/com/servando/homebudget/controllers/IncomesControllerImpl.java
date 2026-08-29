package com.servando.homebudget.controllers;

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
    public ResponseEntity<GenericResponseDto<List<IncomesModel>>> getAll() {
        var results = incomeService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<IncomesModel>> create(@RequestBody @Valid CreateIncomeRequestDto toCreate) {
        var response = incomeService.createIncome(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<IncomesModel>> update(@PathVariable @Valid String id, @RequestBody @Valid UpdateIncomeRequestDto toUpdate) {
        var response = incomeService.updateIncome(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id) {
        var result = incomeService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
