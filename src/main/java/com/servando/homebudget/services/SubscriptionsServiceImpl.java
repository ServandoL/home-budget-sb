package com.servando.homebudget.services;

import com.servando.homebudget.models.database.SubscriptionsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.SubscriptionsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionsServiceImpl extends BaseCrudServiceImpl<SubscriptionsModel, SubscriptionsRepository, CreateSubscriptionsRequestDto, UpdateSubscriptionsRequestDto> {
    SubscriptionsServiceImpl(SubscriptionsRepository repository) {
        super(repository);
    }

    @Cacheable(cacheNames = "subscriptions", key = "#householdId")
    public GenericResponseDto<List<SubscriptionsModel>> findAll(String householdId) {
        var sort = Sort.by(Sort.Direction.ASC, "priorityLevel");
        return super.getAll(householdId, sort);
    }

    @CacheEvict(cacheNames = "subscriptions", key = "#householdId")
    public GenericResponseDto<SubscriptionsModel> createIncome(String householdId, CreateSubscriptionsRequestDto request) {
        var toCreate = new SubscriptionsModel(
                request.getName(),
                request.getBillingCycle(),
                request.getAmount(),
                request.getBillingDay(),
                request.getPriorityLevel()
        );
        return this.create(householdId, request, toCreate);
    }

    @CacheEvict(cacheNames = "subscriptions", key = "#householdId")
    public GenericResponseDto<SubscriptionsModel> updateIncome(String householdId, String id, UpdateSubscriptionsRequestDto request) {
        var other = findOtherById(householdId, id);
        var toUpdate = new SubscriptionsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getAmount(), other.getAmount()),
                ResolveValueFactory.of(request.getBillingDay(), other.getBillingDay()),
                ResolveValueFactory.of(request.getPriorityLevel(), other.getPriorityLevel())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(householdId, id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "subscriptions", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }
}
