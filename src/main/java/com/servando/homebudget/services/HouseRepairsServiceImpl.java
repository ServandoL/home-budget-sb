package com.servando.homebudget.services;

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
    protected HouseRepairsServiceImpl(HouseRepairsRepository repository) {
        super(repository);
    }

    @Cacheable(cacheNames = "house-repairs", key = "'all'")
    public GenericResponseDto<List<HouseRepairsModel>> findAll() {
        var sort = Sort.by(Sort.Direction.ASC, "priority");
        return this.getAll(sort);
    }

    @CacheEvict(cacheNames = "house-repairs", key = "'all'")
    public GenericResponseDto<HouseRepairsModel> createRepair(CreateHouseRepairsDto request) {
        var toCreate = new HouseRepairsModel(
                request.getName(),
                Instant.now()
        );
        toCreate.setDescription(request.getDescription());
        toCreate.setPriority(request.getPriority());
        toCreate.setCost(request.getCost());
        toCreate.setStatus(request.getStatus());
        toCreate.setNotes(request.getNotes());
        toCreate.setDateComplete(request.getDateCompleted());
        toCreate.setCategory(request.getCategory());
        return this.create(request, toCreate);
    }

    @CacheEvict(cacheNames = "house-repairs", key = "'all'")
    public GenericResponseDto<HouseRepairsModel> updateRepair(String id, UpdateHouseRepairsDto request) {
        var other = findOtherById(id);
        var toUpdate = new HouseRepairsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                Instant.now()
        );
        toUpdate.setDescription(ResolveValueFactory.of(request.getDescription(), other.getDescription()));
        toUpdate.setPriority(ResolveValueFactory.of(request.getPriority(), other.getPriority()));
        toUpdate.setCost(ResolveValueFactory.of(request.getCost(), other.getCost()));
        toUpdate.setStatus(ResolveValueFactory.of(request.getStatus(), other.getStatus()));
        toUpdate.setNotes(ResolveValueFactory.of(request.getNotes(), other.getNotes()));
        toUpdate.setDateComplete(ResolveValueFactory.of(request.getDateCompleted(), other.getDateComplete()));
        toUpdate.setCategory(ResolveValueFactory.of(request.getCategory(), other.getCategory()));
        return this.update(id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "house-repairs", key = "'all'")
    public GenericResponseDto<String> delete(String id) {
        return super.delete(id);
    }
}
