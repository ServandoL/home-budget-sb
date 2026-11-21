package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.home.household.HouseholdEvent.Action;
import com.home.household.HouseholdEvents;
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
public class BillsControllerImpl implements CrudController<BillsModel, CreateBillsRequestDto, UpdateBillsRequestDto> {

    @Autowired
    private BillsServiceImpl billsService;

    @Autowired
    private HouseholdEvents events;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<BillsModel>>> getAll(CurrentMember member) {
        var results = billsService.findAll(member.householdId());
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<BillsModel>> create(CurrentMember member, @RequestBody @Valid CreateBillsRequestDto toCreate) {
        var response = billsService.createBill(member.householdId(), toCreate);
        // After the service's @CacheEvict, so other devices refetch fresh data.
        events.publish(member, "bills", Action.CREATED, response.data().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<BillsModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid UpdateBillsRequestDto toUpdate) {
        var response = billsService.updateBill(member.householdId(), id, toUpdate);
        events.publish(member, "bills", Action.UPDATED, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id) {
        var result = billsService.delete(member.householdId(), id);
        events.publish(member, "bills", Action.DELETED, id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
