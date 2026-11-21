package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.home.household.HouseholdEvent.Action;
import com.home.household.HouseholdEvents;
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

    @Autowired
    private HouseholdEvents events;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<CreditCardsModel>>> getAll(CurrentMember member) {
        var results = creditCardsService.findAll(member.householdId());
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<CreditCardsModel>> create(CurrentMember member, @RequestBody @Valid CreateCreditCardsRequestDto toCreate) {
        var response = creditCardsService.createCreditCard(member.householdId(), toCreate);
        // After the service's @CacheEvict, so other devices refetch fresh data.
        events.publish(member, "credit-cards", Action.CREATED, response.data().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<CreditCardsModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid UpdateCreditCardsRequestDto toUpdate) {
        var response = creditCardsService.updateCreditCard(member.householdId(), id, toUpdate);
        events.publish(member, "credit-cards", Action.UPDATED, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id) {
        var result = creditCardsService.delete(member.householdId(), id);
        events.publish(member, "credit-cards", Action.DELETED, id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
