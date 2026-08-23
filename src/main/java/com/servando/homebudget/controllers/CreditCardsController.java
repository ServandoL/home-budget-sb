package com.servando.homebudget.controllers;

import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.services.CreditCardsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-cards")
public class CreditCardsController {

    @Autowired
    private CreditCardsService creditCardsService;

    @GetMapping
    ResponseEntity<GenericResponseDto<List<CreditCardsModel>>> getBills() {
        var results = creditCardsService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    ResponseEntity<GenericResponseDto<String>> createBill(@RequestBody @Valid CreateCreditCardsRequestDto toCreate) {
        var response = creditCardsService.createCreditCard(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<GenericResponseDto<String>> updateBill(@PathVariable @Valid String id, @RequestBody @Valid UpdateCreditCardsRequestDto toUpdate) {
        var response = creditCardsService.updateCreditCard(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<GenericResponseDto<String>> deleteBill(@PathVariable @Valid String id) {
        var result = creditCardsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
