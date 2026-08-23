package com.servando.homebudget.controllers;

import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.dto.CreateBillsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateBillsRequestDto;
import com.servando.homebudget.services.BillsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bills")
public class BillsController {

    @Autowired
    private BillsServiceImpl billsService;

    @GetMapping
    ResponseEntity<GenericResponseDto<List<BillsModel>>> getBills() {
        var results = billsService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    ResponseEntity<GenericResponseDto<String>> createBill(@RequestBody @Valid CreateBillsRequestDto toCreate) {
        var response = billsService.createBill(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<GenericResponseDto<String>> updateBill(@PathVariable @Valid String id, @RequestBody @Valid UpdateBillsRequestDto toUpdate) {
        var response = billsService.updateBill(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<GenericResponseDto<String>> deleteBill(@PathVariable @Valid String id) {
        var result = billsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
