package com.servando.homebudget.services;

import com.servando.homebudget.encryption.FieldCipher;
import com.servando.homebudget.models.database.HouseRepairsModel;
import com.servando.homebudget.models.dto.CreateHouseRepairsDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateHouseRepairsDto;
import com.servando.homebudget.repository.HouseRepairsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class HouseRepairsServiceImpl extends BaseCrudServiceImpl<HouseRepairsModel, HouseRepairsRepository, CreateHouseRepairsDto, UpdateHouseRepairsDto> {
    protected HouseRepairsServiceImpl(HouseRepairsRepository repository, FieldCipher cipher) {
        super(repository, cipher);
    }

    @Cacheable(cacheNames = "house-repairs", key = "#householdId")
    public GenericResponseDto<List<HouseRepairsModel>> findAll(String householdId) {
        var sort = Sort.by(Sort.Direction.ASC, "priority");
        return this.getAll(householdId, sort);
    }

    @CacheEvict(cacheNames = "house-repairs", key = "#householdId")
    public GenericResponseDto<HouseRepairsModel> createRepair(String householdId, CreateHouseRepairsDto request) {
        var toCreate = new HouseRepairsModel(
                request.getName(),
                Instant.now()
        );
        toCreate.setDescription(request.getDescription());
        toCreate.setPriority(request.getPriority());
        toCreate.setCost(request.getCost());
        toCreate.setStatus(request.getStatus());
        toCreate.setNotes(request.getNotes());
        toCreate.setDateComplete(request.getDateComplete());
        toCreate.setCategory(request.getCategory());
        return this.create(householdId, request, toCreate);
    }

    @CacheEvict(cacheNames = "house-repairs", key = "#householdId")
    public GenericResponseDto<HouseRepairsModel> updateRepair(String householdId, String id, UpdateHouseRepairsDto request) {
        var other = findOtherById(householdId, id);
        var toUpdate = new HouseRepairsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                Instant.now()
        );
        toUpdate.setDescription(ResolveValueFactory.of(request.getDescription(), other.getDescription()));
        toUpdate.setPriority(ResolveValueFactory.of(request.getPriority(), other.getPriority()));
        toUpdate.setCost(ResolveValueFactory.of(request.getCost(), other.getCost()));
        toUpdate.setStatus(ResolveValueFactory.of(request.getStatus(), other.getStatus()));
        toUpdate.setNotes(ResolveValueFactory.of(request.getNotes(), other.getNotes()));
        toUpdate.setDateComplete(ResolveValueFactory.of(request.getDateComplete(), other.getDateComplete()));
        toUpdate.setCategory(ResolveValueFactory.of(request.getCategory(), other.getCategory()));
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(householdId, id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "house-repairs", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }
}
