package com.servando.homebudget.controllers;

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

    @GetMapping
    public ResponseEntity<GenericResponseDto<List<HouseRepairsModel>>> getAll() {
        var results = houseRepairService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @PostMapping
    public ResponseEntity<GenericResponseDto<HouseRepairsModel>> create(@RequestBody @Valid CreateHouseRepairsDto toCreate) {
        var response = houseRepairService.createRepair(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericResponseDto<HouseRepairsModel>> update(@PathVariable @Valid String id, @RequestBody @Valid UpdateHouseRepairsDto toUpdate) {
        var response = houseRepairService.updateRepair(id, toUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> delete(@PathVariable @Valid String id) {
        var result = houseRepairService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
