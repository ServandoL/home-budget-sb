package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.home.household.HouseholdEvent.Action;
import com.home.household.HouseholdEvents;
import com.servando.homebudget.models.database.HouseRepairsModel;
import com.servando.homebudget.models.dto.CreateHouseRepairsDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateHouseRepairsDto;
import com.servando.homebudget.services.HouseRepairsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/house-repairs")
public class HouseRepairsControllerImpl implements CrudController<HouseRepairsModel, CreateHouseRepairsDto, UpdateHouseRepairsDto> {
    @Autowired
    private HouseRepairsServiceImpl houseRepairService;

    @Autowired
    private HouseholdEvents events;

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<HouseRepairsModel>>> getAll(CurrentMember member) {
        var results = houseRepairService.findAll(member.householdId());
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<HouseRepairsModel>> create(CurrentMember member, @RequestBody @Valid CreateHouseRepairsDto toCreate) {
        var response = houseRepairService.createRepair(member.householdId(), toCreate);
        // After the service's @CacheEvict, so other devices refetch fresh data.
        events.publish(member, "house-repairs", Action.CREATED, response.data().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<HouseRepairsModel>> update(CurrentMember member, @PathVariable @Valid String id, @RequestBody @Valid UpdateHouseRepairsDto toUpdate) {
        var response = houseRepairService.updateRepair(member.householdId(), id, toUpdate);
        events.publish(member, "house-repairs", Action.UPDATED, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(CurrentMember member, @PathVariable @Valid String id) {
        var result = houseRepairService.delete(member.householdId(), id);
        events.publish(member, "house-repairs", Action.DELETED, id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
