package com.servando.homebudget.services;

import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.database.SubscriptionsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.IncomesRepository;
import com.servando.homebudget.repository.SubscriptionsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionsServiceImpl extends BaseCrudServiceImpl<SubscriptionsModel, SubscriptionsRepository, CreateSubscriptionsRequestDto, UpdateSubscriptionsRequestDto> {
    SubscriptionsServiceImpl(SubscriptionsRepository repository) {
        super(repository);
    }

    public GenericResponseDto<SubscriptionsModel> createIncome(CreateSubscriptionsRequestDto request) {
        var toCreate = new SubscriptionsModel(
                request.getName(),
                request.getBillingCycle(),
                request.getAmount(),
                request.getBillingDay()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<SubscriptionsModel> updateIncome(String id, UpdateSubscriptionsRequestDto request) {
        var other = findOtherById(id);
        var toUpdate = new SubscriptionsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getAmount(), other.getAmount()),
                ResolveValueFactory.of(request.getBillingDay(), other.getBillingDay())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(id, request, toUpdate);
    }
}
