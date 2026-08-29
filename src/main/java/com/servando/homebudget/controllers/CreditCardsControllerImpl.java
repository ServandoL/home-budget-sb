package com.servando.homebudget.controllers;

import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.services.CreditCardsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-cards")
public class CreditCardsControllerImpl implements CrudController<CreditCardsModel, CreateCreditCardsRequestDto, UpdateCreditCardsRequestDto> {

    @Autowired
    private CreditCardsServiceImpl creditCardsService;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<CreditCardsModel>>> getAll() {
        var results = creditCardsService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<CreditCardsModel>> create(@RequestBody @Valid CreateCreditCardsRequestDto toCreate) {
        var response = creditCardsService.createCreditCard(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> update(@PathVariable @Valid String id, @RequestBody @Valid UpdateCreditCardsRequestDto toUpdate) {
        var response = creditCardsService.updateCreditCard(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id) {
        var result = creditCardsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
