package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.home.household.HouseholdEvent.Action;
import com.home.household.HouseholdEvents;
import com.servando.homebudget.models.dto.BudgetSettingsDto;
import com.servando.homebudget.models.dto.UpdateBudgetSettingsDto;
import com.servando.homebudget.services.BudgetSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The household's budget planning settings. Any member can change them, like every budget record. */
@RestController
@RequestMapping("/api/v1/settings")
public class BudgetSettingsController {
    private final BudgetSettingsService settings;
    private final HouseholdEvents events;

    public BudgetSettingsController(BudgetSettingsService settings, HouseholdEvents events) {
        this.settings = settings;
        this.events = events;
    }

    @GetMapping
    public ResponseEntity<BudgetSettingsDto> get(CurrentMember member) {
        return ResponseEntity.ok(settings.get(member.householdId()));
    }

    @PutMapping
    public ResponseEntity<BudgetSettingsDto> save(CurrentMember member, @RequestBody @Valid UpdateBudgetSettingsDto body) {
        var saved = settings.save(member, body);
        events.publish(member, "settings", Action.UPDATED, member.householdId());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping
    public ResponseEntity<BudgetSettingsDto> reset(CurrentMember member) {
        settings.reset(member.householdId());
        events.publish(member, "settings", Action.UPDATED, member.householdId());
        return ResponseEntity.ok(settings.get(member.householdId()));
    }
}
