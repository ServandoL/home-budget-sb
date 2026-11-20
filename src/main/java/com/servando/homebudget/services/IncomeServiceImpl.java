package com.servando.homebudget.services;

import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.IncomesRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncomeServiceImpl extends BaseCrudServiceImpl<IncomesModel, IncomesRepository, CreateIncomeRequestDto, UpdateIncomeRequestDto> {
    IncomeServiceImpl(IncomesRepository repository) {
        super(repository);
    }

    @Cacheable(cacheNames = "incomes", key = "#householdId")
    public GenericResponseDto<List<IncomesModel>> findAll(String householdId) {
        var sort = Sort.by(Sort.Direction.DESC, "netAmount");
        return this.getAll(householdId, sort);
    }

    @CacheEvict(cacheNames = "incomes", key = "#householdId")
    public GenericResponseDto<IncomesModel> createIncome(String householdId, CreateIncomeRequestDto request) {
        var toCreate = new IncomesModel(
                request.getName(),
                request.getBillingCycle(),
                request.getFrequency(),
                request.getNetAmount()
        );
        return this.create(householdId, request, toCreate);
    }

    @CacheEvict(cacheNames = "incomes", key = "#householdId")
    public GenericResponseDto<IncomesModel> updateIncome(String householdId, String id, UpdateIncomeRequestDto request) {
        var other = findOtherById(householdId, id);
        var toUpdate = new IncomesModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getFrequency(), other.getFrequency()),
                ResolveValueFactory.of(request.getNetAmount(), other.getNetAmount())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(householdId, id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "incomes", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }
}
