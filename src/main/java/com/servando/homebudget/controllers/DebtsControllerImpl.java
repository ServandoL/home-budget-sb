package com.servando.homebudget.controllers;

import com.servando.homebudget.models.database.DebtsModel;
import com.servando.homebudget.models.dto.CreateDebtDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateDebtDto;
import com.servando.homebudget.services.DebtsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/debts")
public class DebtsControllerImpl implements CrudController<DebtsModel, CreateDebtDto, UpdateDebtDto> {

    @Autowired
    private DebtsServiceImpl debtsService;

    @Override
    @GetMapping
    public ResponseEntity<GenericResponseDto<List<DebtsModel>>> getAll() {
        var results = debtsService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @Override
    @PostMapping
    public ResponseEntity<GenericResponseDto<DebtsModel>> create(@RequestBody @Valid CreateDebtDto toCreate) {
        var response = debtsService.createDebt(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<DebtsModel>> update(@PathVariable @Valid String id, @RequestBody @Valid UpdateDebtDto toUpdate) {
        var response = debtsService.updateDebt(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id) {
        var result = debtsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
