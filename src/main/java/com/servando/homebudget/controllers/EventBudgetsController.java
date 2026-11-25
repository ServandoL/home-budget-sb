package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.home.household.HouseholdEvent.Action;
import com.home.household.HouseholdEvents;
import com.servando.homebudget.models.database.EventBudgetModel;
import com.servando.homebudget.models.dto.AddEventPurchaseDto;
import com.servando.homebudget.models.dto.CreateEventBudgetDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateEventBudgetDto;
import com.servando.homebudget.services.EventBudgetsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spending limits for outings. Not /events: that's the household's live-update stream.
 * Purchases have their own endpoints, so adding one never sends back a stale copy of the others.
 */
@RestController
@RequestMapping("/api/v1/event-budgets")
public class EventBudgetsController implements CrudController<EventBudgetModel, CreateEventBudgetDto, UpdateEventBudgetDto> {
    private static final String RESOURCE = "event-budgets";

    private final EventBudgetsServiceImpl service;
    private final HouseholdEvents events;

    public EventBudgetsController(EventBudgetsServiceImpl service, HouseholdEvents events) {
        this.service = service;
        this.events = events;
    }

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<EventBudgetModel>>> getAll(CurrentMember member) {
        return ResponseEntity.ok(service.findAll(member.householdId()));
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<EventBudgetModel>> create(CurrentMember member, @RequestBody @Valid CreateEventBudgetDto toCreate) {
        var response = service.createEvent(member.householdId(), toCreate);
        events.publish(member, RESOURCE, Action.CREATED, response.data().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<EventBudgetModel>> update(CurrentMember member, @PathVariable String id, @RequestBody @Valid UpdateEventBudgetDto toUpdate) {
        var response = service.updateEvent(member.householdId(), id, toUpdate);
        events.publish(member, RESOURCE, Action.UPDATED, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable String id) {
        var result = service.delete(member.householdId(), id);
        events.publish(member, RESOURCE, Action.DELETED, id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/purchases")
    public ResponseEntity<GenericResponseDto<EventBudgetModel>> addPurchase(CurrentMember member, @PathVariable String id, @RequestBody @Valid AddEventPurchaseDto purchase) {
        var response = service.addPurchase(member.householdId(), id, purchase);
        events.publish(member, RESOURCE, Action.UPDATED, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/purchases/{purchaseId}")
    public ResponseEntity<GenericResponseDto<EventBudgetModel>> removePurchase(CurrentMember member, @PathVariable String id, @PathVariable String purchaseId) {
        var response = service.removePurchase(member.householdId(), id, purchaseId);
        events.publish(member, RESOURCE, Action.UPDATED, id);
        return ResponseEntity.ok(response);
    }
}
